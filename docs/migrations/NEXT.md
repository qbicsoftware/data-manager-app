# Migration: Next release

**Current pom version:** 1.20.2
**Target release version:** TBD *(renamed to `released/v<version>.md` at release cut)*
**Status:** under development

> **To operators:** This document describes schema changes for the **upcoming,
> unreleased** Data Manager version. It is actively being edited as migrations
> land in the codebase. Do **not** apply any migrations listed here to
> production until the release is published and this file moves to
> `released/v<version>.md`.

For the migration documentation structure, see [`README.md`](README.md).

> **Carried over from the hardening release — not yet satisfied.** These were explicitly deferred by
> [`released/v1.20.4.md`](released/v1.20.4.md) and nothing here changes them. Do not treat their
> absence as a gap in the next release until the owning issue says otherwise.
>
> - **`FEAT-AUTH-FLOWS-05` / #1683** — ORCID-only accounts still have
>   `encryptedPassword IS NULL`. The set-password backfill is a *procedure*, not a script; it needs no
>   schema change because `user_verification_token` already carries the `SET_PASSWORD` purpose. The
>   open execution questions are in [`released/v1.20.4.md`](released/v1.20.4.md) §"Not in this
>   release".
> - **#1691** — per-origin throttling. **The code and the schema impact are complete** (no schema change:
>   origin buckets are rows in the existing `auth_attempt` with `scope = 'LOGIN_ORIGIN'`). What remains
>   is the deployment half, which is not in this repository: HAProxy must *replace* `X-Forwarded-For`,
>   and `server.forward-headers-strategy=native` plus
>   `qbic.auth-throttle.login.origin.enabled=true` must be set. Both ship **off**, so merging this
>   changes nothing in production until an operator acts. See
>   [`docs/operations/installation-and-administration.md`](../operations/installation-and-administration.md)
>   §1.7.
> - **#1669** — retention/purge for `user_verification_token` and `auth_attempt`. Both tables grow
>   without bound until it lands.

---

## Summary of schema changes in this release

> *Append a row here whenever a new incremental script is added. Numbering is
> sequential within the release and determines apply order.*

| # | Script | Description | Risk |
|---|---|---|---|
| — | *(no schema changes yet)* | The hardening release added `user_verification_token` and `auth_attempt`; those are documented in [`released/v1.20.4.md`](released/v1.20.4.md). | — |

Each row links to its incremental script. The sections below expand each entry
with apply / verify / rollback detail.

---

## Application properties changes

*Document any new, removed, or renamed `application.properties` entries here.
If there are none this release, delete this section.*

### New keys for origin-scope throttling (#1691) — all inert by default

`AUTH-NFR-03` asks for throttling per account **and** per request origin. Account scope shipped in
v1.20.4; this release adds the origin half. **Nothing changes on deploy**, because origin scope ships
disabled and the header strategy is unchanged.

| Key | Default | Meaning |
|---|---|---|
| `qbic.auth-throttle.login.origin.enabled` | `false` | **Single switch for every origin bucket** |
| `qbic.auth-throttle.login.origin.max-attempts` | `60` | Failed logins per origin before the origin block |
| `qbic.auth-throttle.login.origin.window` | `PT15M` | Counting window for the login origin bucket |
| `qbic.auth-throttle.login.origin.block-duration` | `PT10M` | How long a login origin block lasts |
| `qbic.auth-throttle.mail-origin.max-attempts` | `20` | Link resends per origin before the origin block |
| `qbic.auth-throttle.mail-origin.window` | `PT15M` | Counting window for the mail origin bucket |
| `qbic.auth-throttle.mail-origin.block-duration` | `PT15M` | How long a mail origin block lasts |
| `qbic.auth-throttle.registration-origin.max-attempts` | `100` | Registrations per origin before the origin block |
| `qbic.auth-throttle.registration-origin.window` | `PT15M` | Counting window for the registration origin bucket |
| `qbic.auth-throttle.registration-origin.block-duration` | `PT10M` | How long a registration origin block lasts |

> **Origin scope now covers the mail-sending flows, not just login.** Discovered in production: login
> was throttled per origin, but a single source could still request password resets for arbitrary
> addresses indefinitely. Account scope cannot bound that — one mail per address means no account
> bucket is ever reused — so the reset, confirmation-resend and registration flows now each write an
> origin bucket. The two resend flows share `MAIL_ORIGIN`; registration has its own, larger bucket
> because it is legitimately bursty. **Still no schema change**: they are more rows in the existing
> `auth_attempt` table with different `scope` values.

Unchanged but now load-bearing: `server.forward-headers-strategy`. It stays **unset (`NONE`)** in this
release. Setting `origin.enabled=true` without `server.forward-headers-strategy=native` is a **startup
failure**, deliberately — see ops doc §1.2/§1.7.

**To turn it on** (only after the HAProxy change below):

```properties
server.forward-headers-strategy=native
qbic.auth-throttle.login.origin.enabled=true
```

**Proxy prerequisite, not a property:** HAProxy must **replace** `X-Forwarded-For`
(`http-request set-header X-Forwarded-For %[src]`). `option forwardfor` alone appends, which leaves the
client's own entry at the front of the list.

**No schema change, so there is no migration script for this.** Origin buckets are additional rows in
the existing `auth_attempt` table with `scope = 'LOGIN_ORIGIN'`; `scope` is an open `varchar` and
`attemptKey` is already an opaque hash. Existing rows are untouched and no backfill is needed.

> **Do not enable this during a release deploy.** It is a separate, independently revertable operator
> step because `server.forward-headers-strategy=native` also changes how absolute links
> (confirmation/reset mail) and the ORCID redirect URI are built. Canary one node first — ops doc §1.7.

### One behaviour change for error notification mail

`spring.mail.*` and `qbic.mail.verify-connection-on-startup` are unchanged. What changes is what the
**logback error-notification appender** does when the SMTP server will not upgrade to TLS.

| | Before | After |
|---|---|---|
| Relay supports STARTTLS | mail delivered, encrypted | mail delivered, encrypted |
| Relay does **not** support STARTTLS | mail delivered **in cleartext**, credentials on the wire | **nothing sent**; the refusal is reported as a send failure |

This closes the same defect class as v1.20.4's `starttls.required` change on the application's own
mail path — but that release only fixed `spring.mail.properties.*`, which **does not reach logback**.
The error-notification channel kept falling back to cleartext. It now requires TLS via a subclass of
logback's `SMTPAppender` (`life.qbic.datamanager.logging.StartTlsRequiredSmtpAppender`), since
logback offers `starttls.enable` but no `required`.

**What an operator should check before deploying:**

- The relay must offer STARTTLS on the submission port. A relay that only speaks cleartext SMTP for
  submission will now produce **no error mail at all**.
- This is deliberate and not a fallback: a mail server that cannot encrypt submission is a deployment
  problem to fix. Watch for the startup gate refusing to boot (if
  `qbic.mail.verify-connection-on-startup` is on, which it is by default) — that is the same defect,
  caught earlier.
- Read the trade honestly: error notification mail is now able to fail silently from a developer's
  perspective. It is not invisible — the failure lands in the application log and in logback's status
  manager — but nobody receives an email saying the email failed. Confirm one error mail arrives after
  the deploy rather than assuming it.

**Known limitation, documented rather than hidden:** the logback appender does not receive
`spring.mail.properties.*`. Only the `spring.mail.*` values named as `<springProperty>` in
`logback-spring.xml` (host, port, username, password) reach it. Adding e.g. `mail.smtp.from` or a
custom timeout to `application.properties` will **not** affect error mail. The TLS requirement is
applied in the appender class, so changing it means editing the class, not the properties file.

### Verifying origin scope once enabled

There is no migration script, but there is a real way for the feature to be **present and inert**:
the client address is still the proxy. Check the table after a few failed logins on the canary node.

```sql
-- Which origin buckets exist at all. All four scopes must appear once the corresponding flows are
-- exercised; a flow whose bucket is missing is still account-scoped only, i.e. unbounded.
SELECT scope, COUNT(*) AS rows_
FROM auth_attempt WHERE scope LIKE '%ORIGIN' GROUP BY scope;

-- How many distinct origins are being tracked. A handful is healthy; exactly 1 means the whole
-- deployment is sharing an origin, i.e. getRemoteAddr() is still the proxy -- STOP and roll back.
SELECT COUNT(DISTINCT attemptKey) AS distinct_origins
FROM auth_attempt WHERE scope = 'LOGIN_ORIGIN'
  AND lastAttemptAt > UTC_TIMESTAMP(6) - INTERVAL 15 MINUTE;

-- What is currently blocked, in either bucket.
SELECT scope, attemptCount, blockedUntil FROM auth_attempt
WHERE blockedUntil > UTC_TIMESTAMP(6);
```

The application log must also contain no `no trusted client origin could be resolved` warning. If it
does, the throttle has degraded to account scope and is telling you so.

---

## Migration #<next>: <title>

*Template — copy this heading and fill it in when a new schema change lands.*

| Field | Value |
|---|---|
| **Story** | <GitHub link> |
| **Feature** | <GitHub link> |
| **ADRs** | <comma-separated ADR links> |
| **Scope** | <what kind of change: new table / alter / index / view / …> |
| **Script** | `sql/migrations/<script-name>.sql` |
| **Target datasource** | <`data_management` or `finance` or both> |

### What it does

<Explanation of the schema change and which ADRs/decisions drive it.>

### Pre-flight

```sql
-- <SQL checks to run before applying.>
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> <datasource> \
    < sql/migrations/<script-name>.sql
```

### Verify

```sql
-- <SQL checks to confirm the migration worked.>
```

### Rollback

```sql
-- <Rollback SQL, if safe. Explicitly call out destructive operations.>
```

### Operator notes

- <Relevant caveats, downtime needed, data movement, etc.>

---

## Release cut procedure

When this release is ready to ship:

1. **Freeze this file.** No further entries after the version is cut.
2. **Confirm the target version.** Update `<version>` in root `pom.xml` if it
   wasn't already; record it here.
3. **Rename:** `NEXT.md` → `released/v<version>.md`
4. **Copy the referenced scripts** into `released/v<version>/` and repoint the
   guide at those copies (a released guide must be self-contained).
5. **Add a release-wide pre-flight section** to the new file if applicable.
6. **Reset `NEXT.md`:** create a fresh empty copy from the template above
   for the *subsequent* release.

See [`README.md`](README.md) for the full migration documentation structure.
