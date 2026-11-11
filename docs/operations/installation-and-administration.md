# Installation & administration

Operational documentation for the Data Manager, for the people who deploy and run it.

**Scope of this document:** what the *deployment environment* must provide. It deliberately does not
explain application internals — for those, follow the links.

| Topic | Where |
|---|---|
| Configuration reference (every property, every env var) | [`README.md`](../../README.md#configuration), [`application.properties.template`](../../application.properties.template) |
| Schema changes per release | [`docs/migrations/`](../migrations/README.md) |
| Security posture and vulnerability handling | [`docs/security/`](../security/), [`SECURITY.md`](../../SECURITY.md) |
| Auth-flow behaviour and known limits | [`docs/plans/HANDOVER-auth-flow-hardening.md`](../plans/HANDOVER-auth-flow-hardening.md) |

---

## 1. Reverse proxy contract

The application is deployed behind a TLS-terminating reverse proxy (HAProxy in the QBiC deployment).
The proxy is not optional infrastructure — **several application features are only correct if the
proxy behaves as described here.** Read this section before going live, and re-read it before
changing the proxy config.

### 1.1 What the proxy MUST do

| # | Requirement | Why |
|---|---|---|
| 1 | Send `X-Forwarded-For` with the real client address | see §1.3 |
| 2 | Send `X-Forwarded-Proto` (`https`) | link URLs, OAuth2 redirect URIs |
| 3 | Send `X-Forwarded-Host` (the public host) | link URLs, OAuth2 redirect URIs |
| 4 | **Overwrite** these headers, never append | see §1.5 — this is the one that silently breaks security |
| 5 | Keep exactly one public origin per environment | WebAuthn `rpId`, OAuth2 redirect URIs |
| 6 | Maintain session affinity (sticky sessions) | Vaadin push and session state |

### 1.2 What the application must be told

By default the application trusts **nothing** — `server.forward-headers-strategy` is `NONE`, so it
ignores `X-Forwarded-*` entirely and believes it is talking to `127.0.0.1:8080`.

To consume the headers, set:

```properties
server.forward-headers-strategy=framework
```

Valid values:

| Value | Meaning | Use when |
|---|---|---|
| `NONE` (default) | Ignore `X-Forwarded-*` headers | Only when the app is reached directly, with no proxy |
| `FRAMEWORK` | The application parses the headers itself (`ForwardedHeaderFilter`) | **The intended value behind HAProxy** |
| `NATIVE` | The servlet container parses them (Tomcat `RemoteIpValve`) | Also valid; requires configuring the valve's trusted-proxy list |

> **⚠️ Do not set this until §1.5 is satisfied.** Turning this on while the proxy appends rather than
> overwrites `X-Forwarded-For` lets any client forge their own source address. See §1.5.

### 1.3 What the client address is used for

| Feature | Uses the client address for | State |
|---|---|---|
| **Auth throttling** (`AUTH-NFR-03`) | A per-origin attempt bucket alongside the per-account one | ⏳ account scope only today; origin scope needs §1.2 + §1.5 |
| Link generation (`{baseUrl}`) | Building absolute confirmation / reset URLs | Works today **only** if the proxy sends the headers and §1.2 is set |
| OAuth2 (ORCID) redirect URI | Must match the browser-visible origin | Same dependency |
| WebAuthn / passkeys (planned) | `rpId` and `allowedOrigins` must match the public origin | Future — see §1.6 |
| Audit logging (planned) | Recording the source of a request | Future |

**The consequence today:** with `NONE`, `request.getRemoteAddr()` returns the *proxy's* address — the
same value for every user in the deployment. No per-origin logic can be correct on that value.

### 1.4 Minimal HAProxy configuration

```haproxy
frontend rdm
    bind *:443 ssl crt /etc/haproxy/certs/rdm.pem

    # Tell the backend who the client really is.
    #
    # `option forwardfor` APPENDS to any existing X-Forwarded-For the client sent.
    # That is why the header must be deleted first (see §1.5).
    http-request del-header X-Forwarded-For
    http-request del-header X-Forwarded-Proto
    http-request del-header X-Forwarded-Host
    option forwardfor

    http-request set-header X-Forwarded-Proto https
    http-request set-header X-Forwarded-Host  rdm.qbic.uni-tuebingen.de

    # Session affinity is required by Vaadin (push + session state).
    default_backend app_servers

backend app_servers
    balance roundrobin
    cookie SERVERID insert indirect nocache
    server app1 10.0.0.11:8080 check cookie s1
    server app2 10.0.0.12:8080 check cookie s2
```

`set-header` replaces; `del-header` before `forwardfor` guarantees the client cannot smuggle a value
in. Both are required — see §1.5.

### 1.5 ⚠️ The failure mode that silently disables security

`X-Forwarded-For` is a **client-supplied header**. A request can arrive with:

```
X-Forwarded-For: 1.2.3.4
```

Two proxy behaviours, two very different outcomes:

| Proxy behaviour | `X-Forwarded-For` the app sees | Consequence |
|---|---|---|
| **Overwrites / deletes then appends** (correct) | `1.2.3.4, <real-client-ip>` → app uses the last hop | Correct |
| **Appends to the client's value** (dangerous) | `1.2.3.4, <real-client-ip>` → app uses the **leftmost** entry | **Attacker controls their own identity** |

If the application parses the *leftmost* entry of an appended header, an attacker sends a different
fake address on every request and gets a **fresh rate-limit bucket each time** — the throttle is
effectively switched off, while appearing to work.

**This is not hypothetical for this application.** Verified in the Spring Framework 7 sources
(`ForwardedHeaderUtils`, used by `ForwardedHeaderFilter`):

```java
String forHeader = headers.getFirst("X-Forwarded-For");
String host = getLeftMostValue(forHeader);   // ← the leftmost entry wins
```

So with `server.forward-headers-strategy=framework`, whatever sits **first** in `X-Forwarded-For`
becomes `request.getRemoteAddr()`. If the client supplied that entry, the attacker controls their own
identity for rate-limiting purposes.

**Therefore:** `del-header X-Forwarded-For` before `option forwardfor`, and do not use
`option forwardfor except ...` patterns that leave a client value in place.

**Verify it, do not assume it.** From outside the deployment:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' \
  -H 'X-Forwarded-For: 203.0.113.77' \
  https://rdm.qbic.uni-tuebingen.de/login
```

Then check what the application recorded. With origin-scope throttling in place (§1.7), the forged
address must **not** appear as its own bucket. Until origin scope exists, verify the header contract
indirectly: the application must see the real client, not `10.x`/`172.x` proxy addresses.

### 1.6 Why this blocks more than throttling

The same three conditions (headers sent, headers trustworthy, §1.2 set) are prerequisites for
passkeys and for reliable ORCID login. See
[`docs/plans/MILESTONE-MFA-self-governed-2fa.md`](../plans/MILESTONE-MFA-self-governed-2fa.md) §1a.

Two specific known issues that this fixes:

- `spring.security.oauth2.client.registration.orcid.redirect-uri` defaults to
  `${ORCID_REDIRECT_URI:{baseUrl}/login/oauth2/code/orcid}`. `{baseUrl}` is derived from the request,
  so behind an untrusted proxy the redirect URI can be built with the wrong scheme/host.
- WebAuthn cannot function at all: browsers refuse `navigator.credentials` outside a secure context,
  and `rpId` must equal the public origin.

### 1.7 Enabling origin-scope throttling (once §1.2 and §1.5 hold)

Origin scope is **not implemented** yet — `AUTH-NFR-03` is satisfied for account scope only, and the
gap is tracked in issue [#1682](https://github.com/qbicsoftware/data-manager-app/issues/1682).

This document deliberately describes the **prerequisite contract** rather than shipping a header
parser: doing the latter before §1.2 and §1.5 are verified would produce code that is correct only on
paper. An origin scope built on an untrusted header is worse than no origin scope, because it looks
like a control.

It needs **no schema change**: `auth_attempt.scope` is an open `varchar` and `attemptKey` is an
opaque hash, so an origin bucket is a second row with a different `scope` value.

When it lands, the deployment contract does not change — it will simply *start using* the client
address that this section already requires. **Do §1.2 and §1.5 first**, or the feature will be
present and ineffective.

### 1.8 Known residual risk while origin scope is missing

Throttling is keyed **per email address** only. Consequence, stated plainly:

> An attacker who knows a victim's address can lock that victim out for `block-duration`
> (default **15 minutes**) by submitting 11 failed logins.

This is not a defect in the implementation — it is the direct cost of account scope without origin
scope, and it is why `AUTH-NFR-03` asks for both. Options, in order of preference:

1. **Implement origin scope** (§1.2 + §1.5 first). Then the attacker's own source is throttled
   separately and the victim's legitimate logins are unaffected.
2. **Shorten `qbic.auth-throttle.login.block-duration`** (e.g. `PT5M`) — reduces the lockout window,
   and weakens the control proportionally.
3. **Raise `qbic.auth-throttle.login.max-attempts`** — same trade-off from the other direction.

Whichever is chosen, the helpdesk needs the unblock procedure in §3 — and needs to know that a
reported lockout may be an attack on that user rather than that user's own typos.

---

## 2. Pre-deployment checklist

Run through this before the first production deployment and after any proxy change.

### Environment

- [ ] `DATAMANAGEMENT_DB_*` and `FINANCE_DB_*` point at the real databases
- [ ] `DATAMANAGEMENT_DB_DDL_AUTO=none` (schema is managed by migrations, never by Hibernate)
- [ ] Vault keystore exists, `DATAMANAGER_VAULT_KEY` and `DATAMANAGER_VAULT_ENTRY_PASSWORD` set
      (entropy > 100 bits is enforced at startup)
- [ ] `MAIL_*` reachable; a test confirmation email arrives
- [ ] `ARTEMIS_*` set and the broker reachable
- [ ] `ORCID_CLIENT_ID` / `ORCID_CLIENT_SECRET` set, and the registered redirect URI matches the
      public origin

### Schema

- [ ] Every migration in the relevant [`docs/migrations/`](../migrations/README.md) guide has been applied
- [ ] `SHOW TABLES LIKE 'user_verification_token'` returns a row
- [ ] `SHOW TABLES LIKE 'auth_attempt'` returns a row

  > **Missing `auth_attempt` breaks login outright**, not gracefully: the code queries it on the first
  > login attempt and the request fails with a SQL error.

### Proxy

- [ ] §1.1 items 1–6 all hold
- [ ] `X-Forwarded-For` is deleted before being set (`del-header` then `option forwardfor`)
- [ ] `server.forward-headers-strategy=framework` is set **only after** the line above is verified
- [ ] Session affinity confirmed by logging in twice through the load balancer

### Behaviour smoke test

- [ ] A confirmation link works **once**, then is dead
- [ ] A password reset link works **once**, then is dead
- [ ] An unknown email and a wrong password produce the **same** login message and **no** 500
- [ ] Garbage in the login field is rejected normally, with no `ERROR` in the log
- [ ] Entering a wrong password does not produce a server error
- [ ] `SELECT COUNT(*) FROM auth_attempt;` **grows** after failed logins

  > If this stays `0` while logins are failing, the throttle is either not deployed or failing
  > silently — see §3.

---

## 3. Troubleshooting

| Symptom | Likely cause | Check |
|---|---|---|
| Every failed login returns HTTP 500 | Throttle write failed (e.g. table missing, no transaction, DB read-only) | Application log for `TransactionRequiredException` / SQL errors; `SHOW TABLES LIKE 'auth_attempt'` |
| Login works, but nobody is ever throttled | `auth_attempt` writes failing silently, or thresholds set very high | `SELECT COUNT(*) FROM auth_attempt`, then `qbic.auth-throttle.*` values |
| A user reports being locked out | Account-scope throttle engaged; **no origin scope yet**, so an attacker can trigger this | `SELECT * FROM auth_attempt WHERE blockedUntil > UTC_TIMESTAMP(6)`; unblock with `DELETE FROM auth_attempt WHERE attemptKey = '<hash>'` — or wait for `blockedUntil` |
| Confirmation/reset links point at the wrong host | Proxy headers missing, or §1.2 not set | Link in the email; compare to the public origin |
| ORCID login fails with a redirect-URI mismatch | `{baseUrl}` derived from an untrusted proxy | Registered redirect URI in the ORCID portal vs. the actual request |
| "Too many concurrent sessions" / random logouts | Session affinity missing | Backend balance config, cookie persistence |

### Manually clearing a throttle block

```sql
-- Identify the bucket. attemptKey is a SHA-256 of 'scope' NUL 'normalised-email',
-- so it cannot be read back as an address; match by scope and time instead.
SELECT scope, attemptCount, windowStartAt, blockedUntil
  FROM auth_attempt
 WHERE blockedUntil IS NOT NULL AND blockedUntil > UTC_TIMESTAMP(6)
 ORDER BY blockedUntil DESC;

-- Clear one bucket (all flows for that key):
DELETE FROM auth_attempt WHERE scope = 'LOGIN' AND attemptKey = '<hash from above>';

-- Or clear an entire flow, e.g. after a misconfigured threshold:
DELETE FROM auth_attempt WHERE scope = 'LOGIN';
```

Deleting a row is safe — it is a rate-limiting counter, not an audit trail.

### Retention

**`auth_attempt` rows are never pruned automatically.** No purge job is scheduled; retention is a
compliance decision tracked in issue [#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669).
The table grows by one row per distinct address that attempts a login or a resend within the window.
It is small, but it is unbounded. If it becomes a concern, prune manually:

```sql
DELETE FROM auth_attempt WHERE lastAttemptAt < UTC_TIMESTAMP() - INTERVAL 30 DAY;
```

(`lastAttemptAt` is indexed for exactly this.)

---

## 4. Upgrading

Apply the schema migrations for each release **before** deploying that release's application code —
see [`docs/migrations/`](../migrations/README.md). Migrations are idempotent, so a re-run is safe.

Pay particular attention to operator notes describing **user-visible** changes. For example, releasing
the tokenised auth links invalidated every confirmation and reset link already sitting in an inbox
(by design, no compatibility window) — support needs the script *"request a new link"* before that
release goes out.
