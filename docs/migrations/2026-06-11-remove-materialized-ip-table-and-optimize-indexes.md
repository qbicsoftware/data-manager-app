# Migration: Remove materialized `ip_measurement_sample_summary` table and optimize indexes

**Date:** 2026-06-11  
**Commit hash:** `ebd32a528..23a01017e`  
**PR:** Linked to Story #1432  
**Scope:** Database schema only — no application code changes required

---

## Context

The `ip_measurement_sample_summary` materialized table was introduced as a performance
optimization for the `v_ip_measurement_sample_json` view. However, it was found to be
inconsistent — deletions from `ip_measurements` were never reflected in the summary
table, leading to stale data.

Since IP measurements have a **1:1 sample-to-measurement relationship**, correlated
subqueries (the same pattern used by `v_ngs_measurement_sample_json` and
`v_pxp_measurement_sample_json`) are performant enough. Removing the materialized
table eliminates the consistency bug entirely.

Additionally, composite indexes `(measurement_id, sample_id)` on all three specific
measurement metadata tables were removed as they were unused by the query plan. The
`sample_id` indexes and FK constraints were standardized across all three tables
(NGS was missing these entirely).

---

## Pre-flight checks

Before running this migration, verify the following on the production database:

```sql
-- 1. Confirm the materialized table exists (if not, skip DROP)
SELECT COUNT(*) AS table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'ip_measurement_sample_summary';

-- 2. Confirm orphan data — are there sample_id values in specific_measurement_metadata_ip
--    that do NOT exist in the sample table? If > 0, investigate before dropping FK.
SELECT COUNT(*) AS orphans
FROM specific_measurement_metadata_ip smm
WHERE smm.sample_id IS NOT NULL
  AND smm.sample_id NOT IN (SELECT sample_id FROM sample);

-- 3. Confirm the sample table charset/collation matches specific_measurement_metadata_ip
--    (both must be identical for the FK to work):
SELECT c1.TABLE_NAME, c1.COLUMN_NAME, c1.CHARACTER_SET_NAME, c1.COLLATION_NAME
FROM information_schema.COLUMNS c1
WHERE c1.TABLE_NAME IN ('specific_measurement_metadata_ip', 'sample')
  AND c1.COLUMN_NAME = 'sample_id';
```

### If the orphan count is > 0

The production database should already have the FK constraint and proper charset
matching (since the table was created via the schema file). If orphans exist:

1. **Investigate** why orphans exist — this should not happen in production unless
   there was a data issue during the table creation.
2. **Resolve** by either deleting orphaned rows or ensuring the missing `sample`
   records exist.
3. **Then** proceed with the migration below.

### If sample table charset differs from specific_measurement_metadata_ip

The schema defines both as `utf8mb4_unicode_ci`. If production's `sample` table is
still on an older charset (e.g., `latin1`), convert it first:

```sql
ALTER TABLE sample
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## Migration steps

Execute all statements in a single transaction if possible (or apply during a
maintenance window if DDL cannot be transactional):

```sql
-- ============================================================================
-- STEP 1: Drop the materialized summary table (if it exists)
-- ============================================================================

DROP TABLE IF EXISTS ip_measurement_sample_summary;
```

```sql
-- ============================================================================
-- STEP 2: Recreate the IP measurement sample JSON view
--           Uses correlated subqueries (same pattern as NGS/PxP views).
-- ============================================================================

CREATE OR REPLACE VIEW v_ip_measurement_sample_json AS
SELECT ip.measurement_id,
       ip.facility,
       ip.mhcAntibody,
       ip.mhcTypingMethod,
       ip.enrichmentMethod,
       ip.lcmsMethod,
       ip.lcColumn,
       ip.dataAcquisition,
       ip.massRange,
       ip.retentionTimeRange,
       ip.chargeRange,
       ip.ionMobilityRange,
       ip.sampleMass,
       ip.sampleVolume,
       ip.cycleFractionName,
       ip.prepDate,
       ip.msRunDate,
       ip.comment,
       ip.instrument,
       ip.instrumentName,
       ip.samplePool,
       ip.measurementCode,
       ip.measurementName,
       ip.IRI,
       ip.label                                                                        AS measurement_label,
       ip.projectId,
       ip.registrationTime,

       /* Per-measurement JSON array of samples (keeps sample fields paired) */
       IFNULL((SELECT JSON_ARRAYAGG(JSON_OBJECT(
               'sample_id', s.sample_id,
               'code', s.code,
               'label', s.label
       )
       )
       FROM specific_measurement_metadata_ip smm
       LEFT JOIN sample s ON s.sample_id = smm.sample_id
       WHERE smm.measurement_id = ip.measurement_id
       ORDER BY s.code),
              JSON_ARRAY())                                                          AS samples_json,

       /* Per-measurement distinct experiment IDs (top-level, not in JSON) */
       (SELECT GROUP_CONCAT(DISTINCT s2.experiment_id ORDER BY s2.experiment_id SEPARATOR ',')
        FROM specific_measurement_metadata_ip smm2
        JOIN sample s2 ON s2.sample_id = smm2.sample_id
        WHERE smm2.measurement_id = ip.measurement_id)                              AS experiment_ids,

       /* Single experiment_id if unique; NULL if mixed or none */
       (SELECT CASE
                   WHEN COUNT(DISTINCT s2.experiment_id) = 1
                       THEN MIN(s2.experiment_id)
                   ELSE NULL END
        FROM specific_measurement_metadata_ip smm2
        JOIN sample s2 ON s2.sample_id = smm2.sample_id
        WHERE smm2.measurement_id = ip.measurement_id)                              AS experiment_id,

       /* Handy count */
       JSON_LENGTH(IFNULL((SELECT JSON_ARRAYAGG(JSON_OBJECT('sample_id', s.sample_id))
                           FROM specific_measurement_metadata_ip smm3
                           LEFT JOIN sample s ON s.sample_id = smm3.sample_id
                           WHERE smm3.measurement_id = ip.measurement_id),
                          JSON_ARRAY()))                                                     AS sample_count,

       /* remote_measurement_data via measurementCode */
       rmd.file_count,
       rmd.file_types,
       rmd.registration_at,
       rmd.total_filesize_bytes,
       rmd.updated_at                                                                AS rmd_updated_at,
       rmd.deleted                                                                   AS rmd_deleted,
       rmd.last_sync_at
FROM ip_measurements ip
         INNER JOIN remote_measurement_data rmd
                    ON rmd.measurement_id = ip.measurementCode;
```

```sql
-- ============================================================================
-- STEP 3: Drop unnecessary composite indexes (measurement_id, sample_id)
-- ============================================================================

ALTER TABLE specific_measurement_metadata_ip DROP INDEX FK_ip_measurement_sample_composite;
ALTER TABLE specific_measurement_metadata_ngs  DROP INDEX FK_936j925a6pi06ojgafesihm5b_composite;
ALTER TABLE specific_measurement_metadata_pxp  DROP INDEX FK_pxp_measurement_sample_composite;
```

```sql
-- ============================================================================
-- STEP 4: Add sample_id indexes for FK performance
-- ============================================================================

-- IP
ALTER TABLE specific_measurement_metadata_ip
  ADD INDEX FK_ip_measurement_sample (sample_id);

-- PxP
ALTER TABLE specific_measurement_metadata_pxp
  ADD INDEX FK_pxp_measurement_sample (sample_id);

-- NGS
ALTER TABLE specific_measurement_metadata_ngs
  ADD INDEX FK_ngs_measurement_sample (sample_id);
```

```sql
-- ============================================================================
-- STEP 5: Add missing FK constraint + sample_id index on NGS table
-- ============================================================================

-- NGS was missing both the FK constraint and the sample_id index.
-- Add the FK constraint (creates the FK and requires an index on sample_id).
-- If the index was already added in Step 4, the FK creation will reuse it.

ALTER TABLE specific_measurement_metadata_ngs
  ADD CONSTRAINT FK_ngs_measurement_sample
    FOREIGN KEY (sample_id) REFERENCES sample(sample_id);
```

---

## Verification

After applying the migration, run these checks:

```sql
-- 1. Confirm the materialized table is gone
SELECT COUNT(*) AS table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'ip_measurement_sample_summary';
-- Expected: 0

-- 2. Confirm the view works
SELECT measurement_id, sample_count
FROM v_ip_measurement_sample_json
WHERE sample_count > 0
LIMIT 5;
-- Expected: rows returned without error

-- 3. Confirm query plan uses eq_ref for sample joins (no full table scans)
EXPLAIN SELECT * FROM v_ip_measurement_sample_json;
-- For the sample table (s, s2), expect:
--   - access_type = eq_ref
--   - key = PRIMARY (sample_id PK)
--   - rows = 1

-- 4. Confirm composite indexes are gone
SELECT index_name, table_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name LIKE 'specific_measurement_metadata_%'
  AND index_name LIKE '%composite%';
-- Expected: empty result

-- 5. Confirm all three tables have the sample_id index
SELECT table_name, index_name, column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN (
    'specific_measurement_metadata_ip',
    'specific_measurement_metadata_ngs',
    'specific_measurement_metadata_pxp'
  )
  AND column_name = 'sample_id'
ORDER BY table_name;
-- Expected: 3 rows, one per table
```

---

## Rollback plan

If the view shows degraded performance after migration, the materialized table can
be re-introduced. However, in that case a delete handler (database trigger or event
subscriber) must also be implemented to maintain consistency.

Reverting Steps 4-5 (removing indexes and the FK) is safe and has no impact on
functionality. Reverting Step 2 (re-creating the old view definition) would require
re-adding the materialized table.
