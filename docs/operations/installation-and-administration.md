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
server.forward-headers-strategy=native
server.tomcat.remoteip.remote-ip-header=X-Forwarded-For
server.tomcat.remoteip.protocol-header=X-Forwarded-Proto
```

Valid values:

| Value | Meaning | Use when |
|---|---|---|
| `NONE` (default) | Ignore `X-Forwarded-*` headers | Only when the app is reached directly, with no proxy |
| `NATIVE` | The servlet container parses them (Tomcat `RemoteIpValve`) | **The only safe value behind HAProxy** |
| `FRAMEWORK` | The application parses the headers itself (`ForwardedHeaderFilter`) | **Never on this stack** — see below |

> **⚠️ `FRAMEWORK` must not be used.** `ForwardedHeaderFilter` takes the **leftmost**
> `X-Forwarded-For` entry with no trust check. HAProxy's `option forwardfor` *appends*, so the client's
> own entry survives at the front and becomes `request.getRemoteAddr()` — the client writes its own
> rate-limit identity. `NATIVE` (`RemoteIpValve`) refuses the header unless the connecting peer is in
> `server.tomcat.remoteip.internal-proxies`, then walks the list right-to-left and discards everything
> client-supplied. This was corrected in #1691; earlier revisions of this document recommended
> `framework` and were wrong.

`server.tomcat.remoteip.internal-proxies` defaults to RFC1918, loopback, link-local, CGNAT and ULA
(`10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.0.0/16, 100.64.0.0/10, 127.0.0.0/8, ::1/128,
fc00::/7, fe80::/10`) — which already covers the QBiC HAProxy nodes. Set it explicitly if the proxy
lives elsewhere.

> **⚠️ Do not set this until §1.5 is satisfied.** Even `NATIVE` trusts the header from any peer inside
> `internal-proxies`, so turning it on before the proxy *replaces* `X-Forwarded-For` is still a
> misconfiguration. See §1.5.

### 1.3 What the client address is used for

| Feature | Uses the client address for | State |
|---|---|---|
| **Auth throttling** (`AUTH-NFR-03`) | A per-origin attempt bucket alongside the per-account one | ✅ implemented (#1691) · off by default; needs §1.2 + §1.5 + §1.7 |
| Link generation (`{baseUrl}`) | Building absolute confirmation / reset URLs | Works today **only** if the proxy sends the headers and §1.2 is set |
| OAuth2 (ORCID) redirect URI | Must match the browser-visible origin | Same dependency |
| WebAuthn / passkeys (planned) | `rpId` and `allowedOrigins` must match the public origin | Future — see §1.6 |
| Audit logging (planned) | Recording the source of a request | Future |

**The consequence today:** with `NONE`, `request.getRemoteAddr()` returns the *proxy's* address — the
same value for every user in the deployment. No per-origin logic can be correct on that value, which is
why origin scope stays inert until §1.2 and §1.5 hold: the resolver answers "unknown" and the throttle
falls back to account scope rather than keying every user on the proxy.

### 1.4 Minimal HAProxy configuration

```haproxy
frontend rdm
    bind *:443 ssl crt /etc/haproxy/certs/rdm.pem

    # Tell the backend who the client really is.
    #
    # `%[src]` is HAProxy's own TCP source and `set-header` REPLACES whatever the client sent, so a
    # client-supplied X-Forwarded-For cannot survive. This is stronger and simpler than
    # `del-header` + `option forwardfor`: `option forwardfor` APPENDS, so it needs the delete and the
    # two can drift apart during an edit. Set-header does both in one line (see §1.5).
    http-request set-header X-Forwarded-For   %[src]
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

`set-header` replaces. Never pair a `del-header` with `option forwardfor` and then edit one of them:
`forwardfor` appends, so the delete is the *only* thing stopping a client from keeping its own entry at
the front of the list — and the front is what `framework` reads. `set-header %[src]` has no such
coupling. See §1.5.

### 1.5 ⚠️ The failure mode that silently disables security

`X-Forwarded-For` is a **client-supplied header**. A request can arrive with:

```
X-Forwarded-For: 1.2.3.4
```

Two proxy behaviours, two very different outcomes:

| Proxy behaviour | `X-Forwarded-For` the app sees | Consequence under `native` | Consequence under `framework` |
|---|---|---|---|
| **Replaces** (`set-header %[src]`) — required | `<real-client-ip>` | Correct | Correct |
| **Appends** (`option forwardfor` without a delete) | `1.2.3.4, <real-client-ip>` | Correct — the valve walks right-to-left and discards `1.2.3.4` | **Attacker controls their own identity** — the filter takes the leftmost entry |

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

**Therefore:** make the proxy **replace** the header (`http-request set-header X-Forwarded-For %[src]`),
**and** use `native`. Replacing alone would also make `framework` safe, and `native` alone survives an
append — but neither is worth betting the control on when both are one line each. Do not use
`option forwardfor except ...` patterns that leave a client value in place.

**Verify it, do not assume it.** From outside the deployment:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' \
  -H 'X-Forwarded-For: 203.0.113.77' \
  https://rdm.qbic.uni-tuebingen.de/login
```

Then check what the application recorded. With origin-scope throttling enabled (§1.7), the forged
address must **not** appear as its own bucket — see the hashed-key comparison in §1.7. Until origin
scope is enabled, verify the header contract indirectly: the application must see the real client, not
`10.x`/`172.x` proxy addresses.

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

Origin scope is **implemented** (#1691) and ships **disabled**. Enabling it is one property, but only
once the two prerequisites above hold on the node you enable it on.

```properties
server.forward-headers-strategy=native
qbic.auth-throttle.login.origin.enabled=true
```

| Property | Default | Meaning |
|---|---|---|
| `qbic.auth-throttle.login.origin.enabled` | `false` | **The single switch for every origin bucket** (see below) |
| `qbic.auth-throttle.login.origin.max-attempts` | `60` | Failed logins from one origin before that origin is blocked |
| `qbic.auth-throttle.login.origin.window` | `PT15M` | Counting window for the login origin bucket |
| `qbic.auth-throttle.login.origin.block-duration` | `PT10M` | How long a login origin block lasts |
| `qbic.auth-throttle.mail-origin.max-attempts` | `20` | Link resends from one origin before that origin is blocked |
| `qbic.auth-throttle.mail-origin.window` | `PT15M` | Counting window for the mail origin bucket |
| `qbic.auth-throttle.mail-origin.block-duration` | `PT15M` | How long a mail origin block lasts |
| `qbic.auth-throttle.registration-origin.max-attempts` | `100` | Registration submissions from one origin before that origin is blocked |
| `qbic.auth-throttle.registration-origin.window` | `PT15M` | Counting window for the registration origin bucket |
| `qbic.auth-throttle.registration-origin.block-duration` | `PT10M` | How long a registration origin block lasts |

> **One switch, three buckets.** `login.origin.enabled` is named after login because that is where the
> work started, but it governs **all** origin buckets. That is deliberate: a per-flow switch would let a
> deployment be origin-scoped for login and silently account-scoped for the flows that actually send
> mail — which is precisely the gap that was found in production.

#### Which flows are covered

| Flow | Account bucket | Origin bucket |
|---|---|---|
| Login | `LOGIN` | `LOGIN_ORIGIN` |
| Password reset request | `PASSWORD_RESET_RESEND` | `MAIL_ORIGIN` |
| Confirmation resend | `EMAIL_CONFIRMATION_RESEND` | `MAIL_ORIGIN` |
| Registration submission | `REGISTRATION` | `REGISTRATION_ORIGIN` |

The two resend flows **share** `MAIL_ORIGIN` on purpose: both send one mail for an arbitrary address,
and separate buckets would hand an attacker a doubled allowance for switching between them.
Registration has its **own, much higher** limit because it is legitimately bursty — at the start of a
semester hundreds of students can register from one campus network.

> **Why the mail buckets are not optional.** Account scope cannot bound these flows at all. The abuse is
> "send one reset to a thousand different addresses": every attempt lands in a *fresh* account bucket,
> so no per-account counter ever reaches its threshold and the application sends a thousand emails.
> With origin scope enabled, the first `mail-origin.max-attempts` are allowed and the rest are refused
> for `block-duration`. With it disabled, nothing bounds them but the relay's own limits.

**A mismatched configuration is a startup failure, not a silent one.** Setting `enabled=true` with any
strategy other than `native` aborts startup with an explanation. That is deliberate: `framework` gives
the client its own bucket key (§1.2) and `none` makes one bucket cover the deployment — both are worse
than account scope alone, and both would otherwise look like working controls.

#### How the two buckets combine

The two acceptance criteria for origin scope pull in opposite directions, so they are not simply OR-ed:

```
refuse ⇔ origin bucket is blocked
       ∨ (account bucket is blocked ∧ this origin has itself produced ≥ max-attempts + 1 failures)
       ∨ (account bucket is blocked ∧ no trusted origin is available)
```

The second clause is the one that matters operationally: **a victim spammed from someone else's origin
can still log in from their own**, which account scope alone cannot deliver (§1.8). The cost is that
while an account is blocked, an origin that has never been seen gets `max-attempts` attempts. That is
the deliberate price of not letting one attacker lock out a chosen user, and it is bounded by the
origin threshold.

Grouping: origin keys use the client's **network**, not the address — IPv4 `/24`, IPv6 `/64`, with
IPv4-mapped IPv6 folded to IPv4 first — so rotating inside an allocation does not buy a fresh bucket.
Keys are hashed exactly like account keys, so `auth_attempt` never holds a readable address.

**Canary it.** Enabling this on one node first (as #1691's Phase C describes) is still the right
rollout, because `server.forward-headers-strategy=native` also changes how absolute links and the
ORCID redirect URI are built. Verify the checklist below on that node before rolling to the others.

It needs **no schema change**: `auth_attempt.scope` is an open `varchar` and `attemptKey` is an
opaque hash, so an origin bucket is a second row with a different `scope` value (`LOGIN_ORIGIN`).

#### Verification checklist (run on the canary node)

- [ ] Confirmation email link contains `https://rdm.qbic.uni-tuebingen.de/...`, not `127.0.0.1:8080`
- [ ] Password-reset link, same
- [ ] ORCID login completes
- [ ] `SELECT scope, COUNT(*) FROM auth_attempt WHERE scope LIKE '%ORIGIN' GROUP BY scope;` shows rows
      for `LOGIN_ORIGIN`, and for `MAIL_ORIGIN` after a password-reset request and a confirmation
      resend. If a flow's bucket stays absent, that flow is still account-scoped only — the exact gap
      that was found in production
- [ ] `SELECT COUNT(*) FROM auth_attempt WHERE scope = 'LOGIN_ORIGIN'
      AND lastAttemptAt > UTC_TIMESTAMP(6) - INTERVAL 15 MINUTE;` stays small. A handful of rows means
      dozens of origins; **one** row means the whole deployment is sharing an origin, i.e.
      `getRemoteAddr()` is still the proxy — **stop and roll back**
- [ ] The application log contains no `no trusted client origin could be resolved` warning. If it does,
      origin scope is silently degraded to account scope

#### The forged-header test

From outside the deployment, send a request with a forged header and confirm the forged address never
becomes a bucket:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' \
  -H 'X-Forwarded-For: 203.0.113.77' \
  https://rdm.qbic.uni-tuebingen.de/login
```

Then check that no origin row corresponds to `203.0.113.77`'s network. Because keys are hashed, compare
by count rather than by value: drive a failed login from a known address, note the new row's
`attemptKey`, then repeat with the forged header from the same address. **The `attemptKey` must be
unchanged** — the forged value was discarded by the proxy and/or the valve.

> **Do §1.2 and §1.5 first.** An origin scope built on an untrusted header is worse than none, because
> it looks like a control.

### 1.8 Known residual risk while origin scope is disabled

With origin scope **off** (the default), throttling is keyed **per email address** only. Consequence,
stated plainly:

> An attacker who knows a victim's address can lock that victim out for `block-duration`
> (default **15 minutes**) by submitting 11 failed logins.

This is not a defect in the implementation — it is the direct cost of account scope without origin
scope, and it is why `AUTH-NFR-03` asks for both. Options, in order of preference:

1. **Enable origin scope** (§1.2 + §1.5 + §1.7). Then a victim logging in from a different origin is
   no longer blocked by someone else's spam. This is the only option that removes the risk rather
   than shortening it, and it is now implemented.
2. **Shorten `qbic.auth-throttle.login.block-duration`** (e.g. `PT5M`) — reduces the lockout window,
   and weakens the control proportionally.
3. **Raise `qbic.auth-throttle.login.max-attempts`** — same trade-off from the other direction.

With origin scope **on**, the lockout risk above is removed, and one residual risk replaces it:

> While an account is blocked by an attack, an origin that has never been seen still gets
> `qbic.auth-throttle.login.max-attempts` attempts against it before it is itself treated as the
> source of the damage. An attacker can therefore still spend a bounded number of guesses per fresh
> origin.

That bound is the deliberate price of §1.7's second clause — the alternative is locking out the victim,
which is the failure being fixed. Lowering `max-attempts` tightens it and raises the support burden for
everyone else.

### 1.8.1 Mail-sending flows and origin scope

The same reasoning applies to the flows that send mail, and there the consequence of leaving origin
scope off is worse than a lockout — it is unbounded outbound mail:

> An attacker who requests a password reset (or a registration, or a confirmation resend) for a
> thousand different addresses from one host sends a thousand emails. Account scope never fires,
> because no address is used twice.

Origin scope bounds that at `mail-origin.max-attempts` (default 20) and
`registration-origin.max-attempts` (default 100) per window, per network. Two residual risks remain:

* **A generous origin limit is still a volume allowance.** 400 registrations per hour from one network
  is permitted by default, because a semester start genuinely looks like that. It is a bound, not a
  block — the relay's own limits are the second line of defence.
* **A large NAT is shared.** All users behind one gateway share a bucket, so a burst of legitimate
  resets from a campus network can briefly throttle resets for everyone on it. The block is short
  (`PT15M`) and the alternative — no origin scope — is unbounded mail.

Whichever mode is deployed, the helpdesk needs the unblock procedure in §3 — and needs to know that a
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
- [ ] The proxy **replaces** `X-Forwarded-For` (`http-request set-header X-Forwarded-For %[src]`) — not
      merely `option forwardfor`, which appends and leaves the client's own entry in front
- [ ] `server.forward-headers-strategy=native` is set **only after** the line above is verified —
      never `framework` (§1.2)
- [ ] Session affinity confirmed by logging in twice through the load balancer
- [ ] If `qbic.auth-throttle.login.origin.enabled=true`: the §1.7 checklist has been walked, and the
      application log shows no `no trusted client origin could be resolved` warning

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
| A user reports being locked out | Account-scope throttle engaged. With origin scope **off**, an attacker can trigger this; with it **on**, an origin block may also be affecting a whole institution | `SELECT scope, attemptCount, blockedUntil FROM auth_attempt WHERE blockedUntil > UTC_TIMESTAMP(6)`; unblock with `DELETE FROM auth_attempt WHERE attemptKey = '<hash>'` — or wait for `blockedUntil` |
| `auth_attempt` has no `LOGIN_ORIGIN` rows although origin scope is enabled | The client address is still the proxy, so the resolver refuses to attribute it | Log for `no trusted client origin could be resolved`; `server.forward-headers-strategy` and the §1.2/§1.5 contract |
| An attacker can send unlimited reset / registration mail by varying the address | Origin scope is off, or the deployment predates the mail buckets — account scope cannot bound this | `SELECT scope, COUNT(*) FROM auth_attempt WHERE scope LIKE '%ORIGIN' GROUP BY scope`; confirm `MAIL_ORIGIN` / `REGISTRATION_ORIGIN` rows appear, and that `login.origin.enabled` is `true` |
| Legitimate users on one network report "too many attempts" for resets | `mail-origin.max-attempts` is below what the network needs, or someone set it near the per-account limit | Raise `qbic.auth-throttle.mail-origin.max-attempts`; it must stay well above `qbic.auth-throttle.resend.max-attempts` |
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
