# Migration: Next release

**Current pom version:** 1.20.3
**Target release version:** TBD *(renamed to `released/v<version>.md` at release cut)*

**Status:** under development

The v1.20.4 security patch is already documented as released — see
[`released/v1.20.4.md`](released/v1.20.4.md). This file therefore describes the release **after**
1.20.4: its schema summary starts empty, while behaviour changes that need an operator note
(e.g. "ORCID is no longer a login") are recorded below.

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
> - **`FEAT-AUTH-FLOWS-05` / #1683** — ~~ORCID-only accounts still have `encryptedPassword IS NULL`.~~
>   **Now resolved, see "ORCID is no longer a login" below.** No schema change was needed: a plain
>   password reset gives such an account a password, verified on a running build (2026-11-17).
> - **#1691** — per-origin throttling. Needs HAProxy and a trusted-proxy configuration; not in this
>   repository.
> - **#1669** — retention/purge for `user_verification_token` and `auth_attempt`. Both tables grow
>   without bound until it lands.

---

## Summary of schema changes in this release

> *Append a row here whenever a new incremental script is added. Numbering is
> sequential within the release and determines apply order.*

| # | Script | Description | Risk |
|---|---|---|---|
| 1 | [`mfa-second-factor.sql`](../../sql/migrations/drafts/mfa-second-factor.sql) | Create `user_second_factor`, `user_recovery_code` and `user_mfa_grace` for the self-governed second factor (MILESTONE-MFA M2-1). No existing table is altered | Medium — three additive tables; destructive to roll back once factors exist |

Each row links to its incremental script. The sections below expand each entry
with apply / verify / rollback detail.

The hardening release's `user_verification_token` and `auth_attempt` are documented in
[`released/v1.20.4.md`](released/v1.20.4.md); nothing in this release changes them.

---

## Migration 1 — second-factor storage and the enforcement grace window

> **Status: staging/test only.** The DDL is in `sql/migrations/drafts/` on purpose — it is runnable
> and reviewable, but **not approved for production until the AGENTS.md §12 sign-off is recorded**
> (tracked on the MFA slice PR). Nothing in the running application reads these tables until the login
> challenge (M4) ships, so applying it early is harmless: no behaviour changes, no downtime, no
> existing table touched.

| Field | Value |
|---|---|
| **Story** | [MILESTONE-MFA](https://github.com/qbicsoftware/data-manager-app/milestone/1) — M2-1 |
| **Feature** | [FEAT-MFA-ENRL](https://github.com/qbicsoftware/data-manager-app/milestone/1) |
| **ADRs** | [ADR 0011](../adr/0011-mfa-authentication-authority.md) |
| **Scope** | Three new tables: `user_second_factor`, `user_recovery_code`, `user_mfa_grace` |
| **Script** | `sql/migrations/drafts/mfa-second-factor.sql` |
| **Target datasource** | `data_management` |

### What it does

1. **`user_second_factor`** — one row per enrolled credential: a TOTP seed as AES-GCM ciphertext, or a
   WebAuthn public credential. A row is either one or the other (`chk_user_second_factor_shape`
   enforces it) and removals are soft deletes, so the audit trail keeps showing that a factor existed
   and when it stopped working.
2. **`user_recovery_code`** — single-use break-glass codes, stored as keyed hashes. The hash is
   globally unique because the login path resolves a code without knowing the account.
3. **`user_mfa_grace`** — one row per account: the window during which an unenrolled account may still
   sign in after the policy switches to `required`. **A missing row means "grant a window", never
   "deny"** — see *Operator notes*.

There is deliberately **no `users` change** and **no per-account policy override**: an override value
can express "not required for this account", which is the permanent exemption
[AUTH-R-01](../requirements.md) rules out (ADR 0011 item 3).

### Vault and configuration provisioning — before the first enrolment

Two secrets are involved, and they must **not** be the same value.

**1. `second-factor-master-key` — the AES-256 key that encrypts TOTP seeds.**

```bash
# 32 bytes, Base64-encoded (44 chars). Do NOT use hex: the application Base64-decodes
# the entry and rejects anything that is not exactly 32 bytes.
openssl rand -base64 32

# Store it once, on one node — the keystore file is shared, like the existing vault entries.
keytool -importpass \
    -alias second-factor-master-key \
    -keystore /path/to/shared/keystore.p12 \
    -storepass "$DATAMANAGER_VAULT_KEY"
```

Deploy the keystore to every node, and make sure the alias matches
`qbic.security.vault.second-factor.key-alias` (`second-factor-master-key` by default).

This key is **separate from `external-credential-master-key`** on purpose: factor material must be
rotatable and restorable without touching stored Invenio credentials, and vice versa.

**2. `RECOVERY_CODE_PEPPER` — the secret that hashes recovery codes.**

```bash
# At least 32 characters; 48 random bytes in Base64 is plenty.
openssl rand -base64 48
```

Set it as an environment variable (or `qbic.security.recovery-code.pepper`). It must **not** be the
vault key: ADR 0011 B1 requires recovery codes to keep working when the vault key is lost, which is the
whole point of having them outside the encrypted-seed path. **Back it up with the application
configuration** — a lost or rotated pepper invalidates every outstanding recovery code.

> **The application still starts if neither secret is provisioned.** The protector resolves its key on
> first use, so a missing alias fails *the enrolment* (closed) rather than the process. That changes in
> M4, when the login path starts depending on factors: provision both secrets **before** the release
> that enables the challenge.

### Pre-flight

```sql
-- 1. MariaDB 10.2 or newer: the shape CHECK constraint is enforced from 10.2.1 on.
SELECT VERSION();

-- 2. None of the three tables exist yet (expected: three rows, each 0).
SELECT 'user_second_factor' AS t, COUNT(*) AS existing FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'user_second_factor'
UNION ALL SELECT 'user_recovery_code', COUNT(*) FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'user_recovery_code'
UNION ALL SELECT 'user_mfa_grace', COUNT(*) FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'user_mfa_grace';

-- 3. The FK target exists with the same charset/collation, or the three foreign keys fail.
SELECT table_name, table_collation
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'users';
-- Expected: utf8mb4_unicode_ci (matches what the script declares)
```

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/drafts/mfa-second-factor.sql
```

The script is idempotent (`CREATE TABLE IF NOT EXISTS`, named constraints), so re-running it against a
partially applied database is safe.

### Verify

```sql
-- 1. All three tables exist with the expected collation.
SELECT table_name, table_collation
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name IN ('user_second_factor','user_recovery_code','user_mfa_grace');
-- Expected: 3 rows, utf8mb4_unicode_ci

-- 2. The users foreign keys are in place (expected: 3 rows).
SELECT constraint_name, table_name
  FROM information_schema.key_column_usage
 WHERE table_schema = DATABASE()
   AND referenced_table_name = 'users'
   AND table_name IN ('user_second_factor','user_recovery_code','user_mfa_grace');

-- 3. The one-factor-shape CHECK is registered (expected: 1).
SELECT COUNT(*) AS check_present
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE()
   AND constraint_name = 'chk_user_second_factor_shape';

-- 4. Behavioural probe: a TOTP row without a secret must be refused by the database.
--    (Run in a transaction and roll back — it is expected to fail.)
START TRANSACTION;
INSERT INTO user_second_factor (userId, type, displayName, createdAt)
SELECT id, 'TOTP', 'probe', NOW(6) FROM users LIMIT 1;   -- must fail: CHECK violation
ROLLBACK;
```

After the first enrolment (M3 page: *Settings → Two-factor authentication*):

```sql
-- 5. Nothing that could be a plaintext seed ended up in the table.
--    A raw Base32 seed is exactly 32 characters from A-Z2-7; ciphertext is Base64 and much longer.
SELECT id, type, CHAR_LENGTH(secretCiphertext) AS length
  FROM user_second_factor
 WHERE type = 'TOTP'
   AND (secretCiphertext IS NULL
        OR secretCiphertext REGEXP '^[A-Z2-7]{32}$'
        OR CHAR_LENGTH(secretCiphertext) < 40);
-- Expected: 0 rows
```

### Rollback

```sql
DROP TABLE IF EXISTS `user_mfa_grace`;
DROP TABLE IF EXISTS `user_recovery_code`;
DROP TABLE IF EXISTS `user_second_factor`;
```

**Safe while nothing is enrolled**, destructive afterwards: every enrolled user must re-enrol, and a
user with no recovery code cannot sign in until an administrator re-enrols them (M6-1). If you have to
roll back with a populated table, copy `user_second_factor` first.

### Operator notes

- **The grace row is the lockout guard.** Do not insert rows by hand. When the policy is `required`,
  an account with **no** row is *granted* a fresh window at its next login; an account with a row whose
  `graceUntil` is in the past is *denied*. A hand-written past date therefore locks a user out, and a
  hand-written future date silently exempts them until it expires.
- **Recovery-code hashes are unique across the whole table**, not per user — the redemption path looks
  a code up without knowing the account. A manual import that reuses a hash will fail.
- **Removed factors are kept** (`disabledAt` set). They stop counting as enrolment immediately but stay
  in their owner's list, and they are not purged: they are the audit trail AUTH-R-04 asks for.
- **Rotation:** rotating the second-factor AES key makes every TOTP factor undecryptable (users fall
  back to recovery codes or admin re-enrolment); rotating `RECOVERY_CODE_PEPPER` invalidates every
  outstanding recovery code. Plan both as user-visible events, not background maintenance.

---

## ORCID is no longer a login (AUTH-R-07, #1677)

**No schema change.** `users.oidcId` and `users.oidcIssuer` are reused unchanged, and the provider
registration path that created passwordless accounts is deleted, not migrated.

**Behaviour change:** ORCID stops being an identity provider. It keeps one role — the iD stored on an
existing account, shown next to the user's name and connected from *Settings → Profile*.

### What users will notice

| Before | After |
|---|---|
| "Login with ORCID" on the login page | email + password only |
| Account created by signing in with ORCID | local registration only; every new account has a password |
| ORCID link was a side effect of a login attempt | explicit Connect / Disconnect in settings |

### Before deploying: one query

```sql
SELECT COUNT(*) AS passwordless_active
  FROM users
 WHERE encryptedPassword IS NULL
   AND active = 1;
```

These are confirmed accounts whose only way in was an ORCID assertion. **They keep working**, because an
active account can always obtain a password through "Forgot your password" — verified end to end on a
running build on 2026-11-17. Record the number so the helpdesk knows the size of the population, and make
sure they have the script below.

There is deliberately **no bulk invitation**. A password must be chosen by the person and arrive through
an address they control; mass-mailing a link would be both unnecessary (the reset flow already does this)
and indistinguishable from phishing.

### Helpdesk script

1. Confirm the caller can receive mail at the account address.
2. Tell them to use **"Forgot your password"** on the login page — once. That both sets a password and, for
   an unconfirmed account, is the only action that would help; for an unconfirmed account no mail is
   dispatched by design.
3. Never ask for, or accept, an ORCID password or any ORCID credentials.
4. If a user has **two accounts holding the same ORCID iD** — possible in a population created by the old
   provider-registration flow — they must sign in to the other account and disconnect the iD there
   before they can connect it to the one they want. There is no self-service way to do it from the first
   account, and no admin path yet. Escalate if they cannot access the other account.

### Configuration: one registry, checked at startup

`qbic.oidc.orcid.issuer` is **new and required**. It has no default and the application refuses to start
without it.

| Property | Meaning |
|---|---|
| `qbic.oidc.orcid.issuer` | the registry whose iDs are stored on accounts: `https://orcid.org` or `https://sandbox.orcid.org`. Must match the OAuth2 provider's registry. |
| `qbic.external-service.person-search.orcid.issuer` | now derived from the above; do not set it independently |

Startup cross-checks the issuer against
`spring.security.oauth2.client.provider.orcid.authorization-uri` and **fails** on a mismatch. This is
deliberate: a deployment previously held accounts linked against *both* registries, because the provider
defaulted to sandbox while the person-search integration defaulted to production, and the value written
on a link was a third setting with a silent sandbox fallback. The result was a user who could not attach
their iD because a stale row under the other registry still held it.

⚠️ **If you change registries** (sandbox → production), existing `users.oidcIssuer` values do not change
with you. A user linked under the old registry keeps that value and will appear to have no link in the new
one. Decide the migration explicitly; do not assume the setting re-points old rows.

### The residual passwordless population, stated honestly

The story's ship criterion reads *"no account exists without a password"*. That cannot be met without
mass mail or deletion, and `AUTH-R-07` forbids deletion. What actually holds after this release:

| Population | Password | Reachable? |
|---|---|---|
| `active = 1` | always set | **yes** — gate: `SELECT COUNT(*) FROM users WHERE encryptedPassword IS NULL AND active = 1` = 0 |
| `active = 0` | may be `NULL` | **no** — and was not reachable before this release either |

An inactive passwordless row is an ORCID registration whose confirmation link was never clicked. Such an
account cannot obtain a password (the reset flow deliberately dispatches nothing for an unconfirmed
address) and cannot log in. It is **not deleted**, and support handles it on request.

Recording this matters because an unachievable gate is a gate that gets quietly ignored. The achievable
invariant is the one above, and it is the one asserted in the verification checklist.

Run both counts on a copy of production before deploying and keep the numbers:

```sql
SELECT COUNT(*) FROM users WHERE encryptedPassword IS NULL AND active = 1;  -- must become 0
SELECT COUNT(*) FROM users WHERE encryptedPassword IS NULL AND active = 0;  -- residual, documented
```

### Verify after deploying

- [ ] The login page shows no ORCID option.
- [ ] A new registration asks for a password and cannot be completed without one.
- [ ] *Settings → Profile* shows either "Connect ORCiD iD" or the linked iD with a Disconnect action.
- [ ] Connecting an iD completes and the iD appears with a working "View public record" link.
- [ ] Disconnecting removes it and the group returns to the Connect state.
- [ ] A passwordless account (from the query above) can set a password via "Forgot your password" and
      then sign in without ORCID.
- [ ] The application starts — a startup failure naming the ORCID issuer is a configuration error, not a
      bug.

### Rollback

Redeploy the previous version. **No schema to undo.** Links created in this release are just
`oidcId`/`oidcIssuer` values that the old code also understood, so they survive a rollback harmlessly. A
rolled-back instance will offer ORCID login again, which is the behaviour that release had.

⚠️ Do **not** "clean up" by nulling `oidcId`/`oidcIssuer`. Those values are the researcher records users
have attached to their accounts, and they are not recoverable by the user in bulk.

---

## Application properties changes

*Document any new, removed, or renamed `application.properties` entries here.
If there are none this release, delete this section.*

### New keys: two MFA secrets, plus one behaviour change for error notification mail

| Property | Environment variable | Purpose |
|---|---|---|
| `qbic.security.vault.second-factor.key-alias` | — (defaults to `second-factor-master-key`) | Vault alias of the AES-256 key that encrypts TOTP seeds |
| `qbic.security.recovery-code.pepper` | `RECOVERY_CODE_PEPPER` | Secret that hashes recovery codes; must **not** be the vault key (ADR 0011 B1) |

Both are documented with provisioning commands under *Migration 1* above. Neither is validated at
startup: a missing key fails the second-factor operation (closed), not the process.

### Behaviour change for error notification mail

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
