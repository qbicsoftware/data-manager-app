# Migration: Next release

**Current pom version:** TBD  
**Target release version:** TBD *(renamed to `released/v<version>.md` at release cut)*  
**Status:** under development

> **To operators:** This document describes schema changes for the **upcoming,
> unreleased** Data Manager version. It is actively being edited as migrations
> land in the codebase. Do **not** apply any migrations listed here to
> production until the release is published and this file moves to
> `released/v<version>.md`.

For the migration documentation structure, see [`README.md`](README.md).

---

## Summary of schema changes in this release

> *Append a row here whenever a new incremental script is added. Numbering is
> sequential within the release and determines apply order.*

| # | Script | Description | Risk |
|---|---|---|---|

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
4. **Add a release-wide pre-flight section** to the new file if applicable.
5. **Reset `NEXT.md`:** create a fresh empty copy from the template above
   for the *subsequent* release.

See [`README.md`](README.md) for the full migration documentation structure.