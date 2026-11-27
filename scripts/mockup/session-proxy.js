#!/usr/bin/env node
/**
 * A cookie-injecting reverse proxy for the mock-up.
 *
 * WHY THIS EXISTS
 *   The application is behind a login form. Screenshot tooling can open a URL
 *   but usually cannot type into a form, so every protected route would record
 *   the login page instead of the screen under review.
 *
 *   This proxy owns port 48013 (the single port browser sidecars can reach),
 *   forwards to the application on 48014, and attaches a session cookie it
 *   obtained itself. The result is that opening http://localhost:48013/... is
 *   already authenticated -- no credentials in the screenshot tool.
 *
 * WHAT IT DOES NOT DO
 *   It is not a security boundary and must never be pointed at a real
 *   deployment. It exists for a local mock-up holding fixture data.
 *
 * USAGE
 *   node session-proxy.js --listen 48013 --target 48014 --email ... --password ...
 */
const http = require('http');
const net = require('net');

function parseArgs(argv) {
  const out = {};
  for (let i = 2; i < argv.length; i += 2) {
    const key = argv[i].replace(/^--/, '');
    out[key] = argv[i + 1];
  }
  return out;
}

const args = parseArgs(process.argv);
const LISTEN = Number(args.listen || 48013);
const TARGET = Number(args.target || 48014);
const HOST = '127.0.0.1';
const EMAIL = args.email || 'dev@qbic.local';
const PASSWORD = args.password || 'DevPassword123!';

/**
 * A keep-alive agent shared by every upstream request.
 *
 * Vaadin pushes with LONG_POLLING, so the client keeps several requests in
 * flight at all times. Without an agent each of those opened a brand-new TCP
 * connection to the application and discarded it, which is socket churn on
 * every navigation -- the whole point of this proxy is to stay out of the way.
 */
const agent = new http.Agent({ keepAlive: true, maxSockets: 64 });

let cookie = '';

/** Minimal request helper: resolves with { status, headers, body }. */
function request(path, { method = 'GET', headers = {}, body } = {}) {
  return new Promise((resolve, reject) => {
    const req = http.request({ host: HOST, port: TARGET, path, method, headers, agent }, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body: data }));
    });
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

/**
 * Logs in and returns the JSESSIONID.
 *
 * The login is a normal form POST, so it needs the session cookie from the GET
 * and the CSRF token embedded in the page. Both come from the same request.
 */
async function login() {
  const page = await request('/login', { headers: { cookie: cookie || '' } });
  const setCookie = page.headers['set-cookie'] || [];
  const jsession = setCookie.map((c) => c.split(';')[0]).find((c) => c.includes('JSESSIONID'));
  if (jsession) cookie = jsession;

  const match = page.body.match(/name="_csrf" content="([^"]+)"/);
  if (!match) throw new Error('no CSRF token on the login page');
  const csrf = match[1];

  const form = new URLSearchParams({ username: EMAIL, password: PASSWORD, _csrf: csrf }).toString();
  const result = await request('/login', {
    method: 'POST',
    headers: {
      cookie,
      'content-type': 'application/x-www-form-urlencoded',
      'content-length': Buffer.byteLength(form),
    },
    body: form,
  });

  const postCookies = result.headers['set-cookie'] || [];
  const after = postCookies.map((c) => c.split(';')[0]).find((c) => c.includes('JSESSIONID'));
  if (after) cookie = after;

  // A 302 to "/" means accepted; a 302 to "/login?error" means rejected.
  const location = result.headers.location || '';
  if (location.includes('error')) {
    throw new Error('login rejected; check the fixture account exists and is active');
  }
  return cookie;
}

function forwardHeaders(incoming) {
  const headers = {};
  for (const [key, value] of Object.entries(incoming)) {
    // Replace the caller's cookies with ours: the session we hold is the point.
    if (key.toLowerCase() === 'cookie') continue;
    headers[key] = value;
  }
  if (cookie) headers['cookie'] = cookie;
  return headers;
}

/**
 * True when a response means our session is no longer valid.
 *
 * Spring rotates the session id on login and expires the old one, so anything
 * that posts credentials through this proxy invalidates the cookie we hold. The
 * symptom is a redirect to the login page, so that is what we look for.
 */
function sessionLost(status, headers) {
  const location = headers && headers.location;
  return status === 302 && typeof location === 'string' && location.includes('/login');
}

const server = http.createServer((req, res) => {
  const upstream = http.request(
    { host: HOST, port: TARGET, method: req.method, path: req.url, headers: forwardHeaders(req.headers), agent },
    (upRes) => {
      if (sessionLost(upRes.statusCode, upRes.headers) && !req.url.startsWith('/login')) {
        // Re-authenticate once, then let the client retry. Doing it here keeps
        // the failure invisible to whatever is taking screenshots.
        upRes.resume();
        login()
          .then(() => {
            const retry = http.request(
              { host: HOST, port: TARGET, method: req.method, path: req.url, headers: forwardHeaders(req.headers), agent },
              (again) => {
                res.writeHead(again.statusCode, again.headers);
                again.pipe(res);
              },
            );
            retry.on('error', () => {
              res.writeHead(502, { 'content-type': 'text/plain' });
              res.end('mock-up proxy: retry after re-login failed');
            });
            req.pipe(retry);
          })
          .catch(() => {
            res.writeHead(502, { 'content-type': 'text/plain' });
            res.end('mock-up proxy: session lost and re-login failed; restart run.sh');
          });
        return;
      }
      res.writeHead(upRes.statusCode, upRes.headers);
      upRes.pipe(res);
    },
  );
  upstream.on('error', (e) => {
    res.writeHead(502, { 'content-type': 'text/plain' });
    res.end('mock-up proxy could not reach the application: ' + e.message);
  });
  req.pipe(upstream);
});

// Vaadin keeps a websocket open for server push; without relaying the upgrade
// the UI stops updating after the first render.
server.on('upgrade', (req, socket, head) => {
  const upstream = net.connect(TARGET, HOST, () => {
    let raw = req.method + ' ' + req.url + ' HTTP/1.1\r\n';
    for (const [key, value] of Object.entries(forwardHeaders(req.headers))) {
      raw += key + ': ' + value + '\r\n';
    }
    upstream.write(raw + '\r\n');
    if (head && head.length) upstream.write(head);
    upstream.pipe(socket);
    socket.pipe(upstream);
  });
  upstream.on('error', () => socket.destroy());
  socket.on('error', () => upstream.destroy());
});

(async () => {
  // The application may still be starting; retry rather than fail the run.
  for (let attempt = 1; attempt <= 40; attempt++) {
    try {
      await login();
      console.log('session-proxy: logged in as ' + EMAIL);
      break;
    } catch (e) {
      if (attempt === 40) {
        console.error('session-proxy: could not log in: ' + e.message);
        process.exit(1);
      }
      await new Promise((r) => setTimeout(r, 1000));
    }
  }
  server.listen(LISTEN, HOST, () => {
    console.log('session-proxy: http://localhost:' + LISTEN + ' -> ' + TARGET);
  });
})();
