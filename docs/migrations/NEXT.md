# Migration: Next release

**Current pom version:** 1.20.5
**Target release version:** TBD *(renamed to `released/v<version>.md` at release cut)*
**Status:** under development

> **To operators:** This document describes schema changes for the **upcoming,
> unreleased** Data Manager version. It is actively being edited as migrations
> land in the codebase. Do **not** apply any migrations listed here to
> production until the release is published and this file moves to
> `released/v<version>.md`.

For the migration documentation structure, see [`README.md`](README.md).

> **Carried over from v1.20.5 — not yet satisfied.** These were explicitly deferred by
> [`released/v1.20.5.md`](released/v1.20.5.md) and nothing here changes them. Do not treat their
> absence as a gap in the next release until the owning issue says otherwise.
>
> - **`FEAT-AUTH-FLOWS-05` / #1683** — ORCID-only accounts still have
>   `encryptedPassword IS NULL`. The set-password backfill is a *procedure*, not a script; it needs no
>   schema change because `user_verification_token` already carries the `SET_PASSWORD` purpose. The
>   open execution questions are in [`released/v1.20.4.md`](released/v1.20.4.md) §"Not in this
>   release".
> - **#1691** — per-origin throttling is implemented and ships **off**. What remains is the
>   deployment half, which is not in this repository: HAProxy must *replace* `X-Forwarded-For`,
>   and `server.forward-headers-strategy=native` plus
>   `qbic.auth-throttle.login.origin.enabled=true` must be set. See
>   [`released/v1.20.5.md`](released/v1.20.5.md) and
>   [`docs/operations/installation-and-administration.md`](../operations/installation-and-administration.md)
>   §1.7.
> - **#1669** — retention/purge for `user_verification_token` and `auth_attempt`. Both tables grow
>   without bound until it lands.

---

## Summary of schema changes in this release

| # | Script | Description | Risk |
|---|---|---|---|
| — | *(no schema changes yet)* | v1.20.5 added no tables, columns, indexes or views; its guide is [`released/v1.20.5.md`](released/v1.20.5.md). | — |

**Already released, not pending.** `user_verification_token` (`auth-flow-hardening.sql`) and
`auth_attempt` (`auth-attempt-throttling.sql`) shipped with v1.20.4, together with the JobRunr 9.0.x
bundled migration. Their apply / verify / rollback detail lives in
[`released/v1.20.4.md`](released/v1.20.4.md) and is **not** repeated here — a duplicate copy would
drift. The scripts remain in `sql/migrations/` because they are still part of the schema a fresh
install needs; see [`sql/migrations/README.md`](../sql/migrations/README.md) for the numbering.

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
