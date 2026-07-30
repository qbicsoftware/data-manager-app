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
| 2 | [`sql/migrations/create-user-external-credential.sql`](../sql/migrations/create-user-external-credential.sql) | Create `user_external_credential` table for storing per-user tokens to external providers (FEAT-DATSET-14) | low — new empty table; requires vault master key provisioned on all HA nodes before application starts |

Each row links to its incremental script. The sections below expand each entry
with apply / verify / rollback detail.

---

## Migration #1: `associated_dataset` table

| Field | Value |
|---|---|
| **Story** | [#1467 — FEAT-DATSET-01: Connecting open, published datasets](https://github.com/qbicsoftware/data-manager-app/issues/1467) |
| **Feature** | [#1466 — FEAT-DATASET-CONNECTION](https://github.com/qbicsoftware/data-manager-app/issues/1466) |
| **ADRs** | [0001](../adr/0001-associated-datasets-domain-model.md), [0002](../adr/0002-invenio-rdm-api-client-credentials.md), [0003](../adr/0003-connection-lifecycle-stewardship.md) |
| **Scope** | Database schema only (new table) |
| **Script** | [`sql/migrations/create-associated-dataset.sql`](../sql/migrations/create-associated-dataset.sql) |
| **Target datasource** | `data_management` |

### What it does

Creates an empty `associated_dataset` table. Per [ADR-0001](../adr/0001-associated-datasets-domain-model.md):

- Lives in the `project-management` bounded context (table name starts with `associated_dataset`, not under the existing `dataset_*` namespace, to keep the InvenioRDM connection concept distinct from the legacy OpenBIS raw-data tables).
- Has four **universal columns** (`title`, `pid`, `version`, `publication_date`) duplicated from the JSON blob for SQL sort/filter.
- Stores **source-specific metadata** (InvenioRDM creators, community, access details) in a MariaDB `JSON` column (`resource_metadata`). At the expected scale of <100 rows/project, this is efficient; the escape hatch for future scale is a generated virtual column + index — no entity change required.
- **Soft-delete for removal** (ADR-0001, ADR-0003): deleting a connection sets `connection_state = 'REMOVED'`, leaving the row as an audit tombstone. Active query paths filter `REMOVED` out. There is no `DELETE` statement in v1.
- **No SQL FK constraint on `experiment_id`** — the application enforces referential integrity in memory so that project-ACL changes and experiment deletions don't cascade-delete dataset connections.

### What is NOT in this migration

A second table, `user_external_credential` (for per-user Personal Access
Tokens to access restricted datasets on external providers), is added
as **Migration #2 below** in this same release (FEAT-DATSET-14). It is
intentionally **not** created by this migration because FEAT-DATSET-01
covers only public, open datasets. See Migration #2 for the credential
table definition and its vault-provisioning pre-flight requirement.

### Pre-flight

Run these checks against the target database **before** applying:

```sql
-- 1. Confirm the table does NOT already exist
SELECT COUNT(*) AS table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'associated_dataset';
-- Expected: 0

-- 2. Confirm target datasource charset/collation matches the rest of the schema
SELECT @@character_set_database AS charset,
       @@collation_database    AS collation;
-- Expected: utf8mb4, utf8mb4_unicode_ci

-- 3. Confirm you are connected to the data-management datasource
SELECT DATABASE();
-- Expected: data_management (or whatever your DM schema is named)
```

If the table already exists (return value = 1), this migration was already
applied — no further action required (the DDL is idempotent).

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-associated-dataset.sql
```

Or inline:

```sql
-- The full DDL is inlined in the script for review; see
-- sql/migrations/create-associated-dataset.sql
```

### Verify

```sql
-- 1. Confirm 15 columns
SELECT COUNT(*) AS column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'associated_dataset';
-- Expected: 15

-- 2. Confirm indexes are in place (4 keys + PK = 5 rows)
SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'associated_dataset'
GROUP BY index_name
ORDER BY index_name;
-- Expected rows:
--   PRIMARY                       id
--   idx_assoc_ds_project          project_id
--   idx_assoc_ds_project_state    project_id,connection_state
--   idx_assoc_ds_source_type      source_type
--   idx_assoc_ds_state            connection_state

-- 3. Confirm the application can query the table
SELECT COUNT(*) AS active_connected_count
FROM associated_dataset
WHERE connection_state <> 'REMOVED';
-- Expected: 0 (empty table immediately after migration)

-- 4. Confirm no unintended table was created on the finance datasource
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_name = 'associated_dataset';
-- Expected: exactly one row, for the data-management schema
```

### Rollback

Rollback is destructive — any rows inserted by the application after the
migration will be lost. Because FEAT-DATSET-01 is the first release introducing
this table, rollback is only meaningful before any datasets have been connected
by users.

```sql
-- DANGER: drops all connected-dataset connections.
-- Safe only when the table is empty (see rollback plan below).
DROP TABLE IF EXISTS `associated_dataset`;
```

If the table contains connected-dataset rows, rollback requires one of:

1. **Reverse the feature deployment** to a version that does not reference the
   table, then drop the table.
2. **Truncate the table**, then drop it. Both cases lose all connection data.

In either case, notify users that previously connected datasets will no longer
appear in their projects.

### Operator notes

- **No data is moved.** This migration only creates an empty table.
- **No downtime required.** `CREATE TABLE IF NOT EXISTS` on MariaDB takes
  milliseconds and does not lock other tables.
- **No foreign key constraint is created** between
  `associated_dataset.experiment_id` and `experiments_datamanager.id`. The
  application enforces referential integrity in memory.
- **Future migration (same feature):** Stories 14/15 will add the
  `user_invenio_rdm_credential` table. Expect a follow-up entry in this file
  (or in a subsequent release's `NEXT.md`).

---

## Migration #2: `user_external_credential` table

| Field | Value |
|---|---|
| **Story** | [#1478 — FEAT-DATSET-14: Add credentials for an InvenioRDM instance](https://github.com/qbicsoftware/data-manager-app/issues/1478) |
| **Feature** | [#1466 — FEAT-DATASET-CONNECTION](https://github.com/qbicsoftware/data-manager-app/issues/1466) |
| **ADRs** | [0002](../adr/0002-invenio-rdm-api-client-credentials.md), [0003](../adr/0003-connection-lifecycle-stewardship.md) |
| **Scope** | Database schema (new table) + vault provisioning |
| **Script** | [`sql/migrations/create-user-external-credential.sql`](../sql/migrations/create-user-external-credential.sql) |
| **Target datasource** | `data_management` |

### What it does

Creates an empty `user_external_credential` table for storing per-user Personal
Access Tokens to external data source instances (e.g., InvenioRDM instances like
Zenodo, FDAT). Per [ADR-0002](../adr/0002-invenio-rdm-api-client-credentials.md):

- **Source-agnostic design**: includes `source_type` column alongside
  `instance_id`, allowing future provider types without schema changes. Today
  the only rows will be `INVENIO_RDM` / `zenodo` | `fdat`.
- **Per-user, per-instance tokens**: unique constraint on `(user_id,
  source_type, instance_id)` — one token per user per instance.
- **Encrypted at rest**: `encrypted_token` column stores AES-256-GCM blobs
  (nonce ‖ ciphertext ‖ tag). The AES-256 master key is stored in the PKCS12
  vault (shared keystore across all HA nodes) and protected by the shared entry
  password (`DATAMANAGER_VAULT_ENTRY_PASSWORD`). This is the same shared model
  used for OpenBIS credentials — the blast radius for a compromised entry
  password is consistent with the existing application design.
- **Status tracking**: `status` column tracks `VALID` / `INVALIDATED` lifecycle
  states (updated only on explicit user action, per ADR-0002 §9).

### What is NOT in this migration

- No data is inserted — the table starts empty and is populated by users at
  runtime through the `/external-providers` page.
- No foreign key constraint is added; the application enforces referential
  integrity in memory.

### Vault provisioning requirement

**This migration requires a new vault entry to be provisioned by ops before the
application starts.**

The application reads the master AES-256 key from the PKCS12 vault at startup.
If the entry is missing, the application will fail to start with a clear error
message.

**What ops needs to do:**

1. **Add a new entry to the PKCS12 keystore** under the alias
   `external-credential-master-key`:

   ```bash
   # Generate a 32-byte key and Base64-encode it
   AES_KEY=$(openssl rand -base64 32)
   
   # Store in keystore (run once, on one node — the keystore file is shared)
   echo "$AES_KEY" | keytool -importpass \
       -alias external-credential-master-key \
       -keystore /path/to/shared/keystore.p12 \
       -storepass $DATAMANAGER_VAULT_KEY \
       -keypass $DATAMANAGER_VAULT_ENTRY_PASSWORD \
       -storetype PKCS12
   ```

   **Key format**: The vault entry **must** contain a Base64-encoded string
   representing exactly 32 bytes (256 bits). Use:
   - `openssl rand -base64 32` (produces 44-char Base64 string encoding 32 bytes)

   The application decodes this Base64 string at startup and validates the
   decoded length is exactly 32 bytes. Non-Base64 strings and incorrectly
   sized keys will cause the application to fail-fast with a clear error.

   ⚠️ **Important**: Do not use hex encoding (`openssl rand -hex 32`) — the
   application expects Base64. A 64-char hex string represents only 32 bytes
   after hex-decoding, but the application's Base64 decoder will reject it
   as invalid.

2. **Deploy the keystore** to all HA nodes. The existing vault deployment
   pattern (shared keystore file across nodes) applies here — no new
   distribution mechanism needed.

3. **Verify the alias name** in `application.properties` matches:

   ```properties
   qbic.security.vault.external-credential.key-alias=external-credential-master-key
   ```

### Pre-flight

Run these checks against the target database **before** applying:

```sql
-- 1. Confirm the table does NOT already exist
SELECT COUNT(*) AS table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'user_external_credential';
-- Expected: 0

-- 2. Confirm target datasource charset/collation matches the rest of the schema
SELECT @@character_set_database AS charset,
       @@collation_database    AS collation;
-- Expected: utf8mb4, utf8mb4_unicode_ci

-- 3. Confirm you are connected to the data-management datasource
SELECT DATABASE();
-- Expected: data_management (or whatever your DM schema is named)
```

If the table already exists (return value = 1), this migration was already
applied — no further action required (the DDL is idempotent).

### Apply

```bash
mysql -u <user> -h <host> -P <port> data_management \
    < sql/migrations/create-user-external-credential.sql
```

### Verify

```sql
-- 1. Confirm 8 columns
SELECT COUNT(*) AS column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'user_external_credential';
-- Expected: 8

-- 2. Confirm indexes are in place (3 keys + PK = 4 rows)
SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'user_external_credential'
GROUP BY index_name
ORDER BY index_name;
-- Expected rows:
--   PRIMARY                    id
--   idx_cred_user_src          user_id,source_type
--   idx_cred_user              user_id
--   uk_user_src_instance       user_id,source_type,instance_id

-- 3. Confirm no unintended table was created on the finance datasource
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_name = 'user_external_credential';
-- Expected: exactly one row, for the data-management schema
```

### Rollback

Rollback is destructive — any rows inserted by the application after the
migration will be lost. Because FEAT-DATSET-14 is the first release introducing
this table, rollback is only meaningful before any credentials have been added
by users.

```sql
-- DANGER: drops all stored credentials. Users will need to re-add their tokens.
DROP TABLE IF EXISTS `user_external_credential`;
```

If the table contains credential rows, rollback requires one of:

1. **Reverse the feature deployment** to a version that does not reference the
   table, then drop the table. All users will lose their configured tokens.
2. **Truncate the table**, then drop it. Both cases lose all credential data.

In either case, notify users that their configured InvenioRDM tokens will need
to be re-added after the rollback.

**Note:** The vault entry (`external-credential-master-key`) can remain in the
PKCS12 keystore after rollback — it is harmless if unused.

### Operator notes

- **No data is moved.** This migration only creates an empty table.
- **No downtime required.** `CREATE TABLE IF NOT EXISTS` on MariaDB takes
  milliseconds and does not lock other tables.
- **Vault provisioning is required** before the application can start. See
  "Vault provisioning requirement" above.
- **Future providers:** The `source_type` column allows adding new provider
  types (e.g., LIMS) without schema changes — only application configuration
  changes are needed.

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
