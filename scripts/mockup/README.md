# Mock-up runtime

Start the Data Manager as a self-contained, already-logged-in application so a
screen can be opened and screenshotted without provisioning anything.

```sh
./scripts/mockup/run.sh          # build the database if needed, then start
./scripts/mockup/capture.sh      # print the routes that are ready to screenshot
./scripts/mockup/run.sh --clean  # start over, including the vault
```

Then open <http://localhost:48013/projects/list>. It is authenticated as a
fixture user; there is no login step for the person or tool taking the
screenshot.

## What it replaces

Booting the application normally requires two MariaDB databases, an Artemis
broker, an SMTP server, a PKCS12 vault holding a specific AES key, and eight
environment variables with no defaults. Each of those is a way to fail before
anything is drawn. The mock-up substitutes an embedded H2 database built from
the repository's own `sql/complete-schema.sql`, the embedded Artemis broker the
application already ships, a throwaway vault, and fixture rows.

## The pieces

| File | Purpose |
|---|---|
| `run.sh` | Entry point: resolves a JDK, builds the database, starts the app and the proxy. |
| `schema-to-h2.js` | Rewrites the MariaDB DDL into the subset H2 accepts. 56 tables and 9 views load. |
| `MockDatabase.java` | Loads the translated schema into H2. |
| `Fixtures.java` | Seeds two accounts, three projects, and the ACL rows that make them visible. |
| `VaultKey.java` | Provisions the AES key the app demands in the PKCS12 vault. |
| `session-proxy.js` | Logs in once and forwards the session, so any URL is already authenticated. |
| `routes.txt` | Named routes for review. |

## What was actually verified

These are not assumptions; each was the difference between a working start and
a specific failure:

- **The real schema runs on H2.** 56/56 tables, 9/9 views. Four MariaDB-only
  constructs had to be rewritten first (column `COMMENT`, `json_valid` checks,
  inline `KEY` declarations, `b'0'` literals).
- **`vaadin.productionMode=true` is required.** Without it the app serves an
  empty shell, because the development bundle expects a Vite dev server on
  another port.
- **The session cookie must lose both of its production defaults.** With
  `Secure=true` on plain HTTP, or `Domain=de.uni-tuebingen.qbic` on localhost, a
  browser silently refuses the cookie and the app shows "Cookies disabled".
- **Login is by email address, not username.** The form field is called
  `username`, which makes this easy to get wrong.
- **Passwords are PBKDF2WithHmacSHA256, 100000 iterations.** SHA512 produces a
  hash the application silently rejects.
- **A user ACL sid holds the user's UUID, not their username** — the
  `project_userinfo` view joins `acl_sid.sid` onto `users.id`.
- **The fixture accounts need a `user_role` row**, or the overview resolves no
  accessible projects and renders the empty state.
- **`./tmp` must exist in the working directory**, and Vaadin wants a writable
  home; the script sets both.
- **Stopping the app by port does not work on this platform.** Tomcat binds an
  IPv6 socket and `lsof -ti:PORT` finds no pid for it, so the previous run
  survived every restart and the orphans then competed for the database — which
  is what made navigation feel slow. The pids are now recorded and stopped
  explicitly.

## If navigation feels slow

Check how many instances are running. An orphaned JVM from an earlier run keeps
the previous database open and competes for it, and the application then feels
sluggish without anything obviously failing:

```sh
lsof +D .mockup | grep -c java     # want 1
```

`run.sh` records the pids it starts in `.mockup/app.pid` and
`.mockup/proxy.pid` and stops them on the next run. It deliberately does not
look the processes up by port: Tomcat binds an IPv6 socket here (`netstat`
shows `tcp46 *.48014`) and `lsof -ti:48014` returns nothing for it, so a
port-based lookup silently matched nothing and every restart leaked another
JVM. If you find a pile of them, stop the one in `app.pid` and start again.

The session proxy is optional (`--no-proxy`). It exists so a tool that cannot
fill in a login form can still reach a protected page, and it keeps a keep-alive
connection to the application so `LONG_POLLING` push traffic does not churn
sockets — but it is still one extra hop, so if you are driving the app with a
real browser, `--no-proxy` is the faster and simpler choice.

## What is not covered

- **Only the project overview has fixture depth.** The three projects exist as
  rows; experiments, samples and measurements do not. Project detail routes
  render, but with empty content. See the commented entries in `routes.txt`.
- **Not every route is checked.** A route that does not resolve renders the
  not-found page, which looks like a screenshot and reports nothing. Add a route
  to `routes.txt` only after confirming it resolves.
- **The proxy is not a security boundary.** It holds a session and hands it to
  anything that connects to port 48013. Local mock-up only; never point it at a
  real deployment.
- **The schema translation is not exhaustive.** It covers what
  `complete-schema.sql` contains today. A new MariaDB-only construct will fail
  the load loudly, naming the statement — extend `schema-to-h2.js` rather than
  editing the production schema.
