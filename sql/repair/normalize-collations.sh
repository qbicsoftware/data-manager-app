#!/usr/bin/env bash
#
# INSTANCE REPAIR, STEP 2: convert a schema to utf8mb4_unicode_ci.
#
# Companion to sql/repair/normalize-collations.sql (rationale, inspection, view recreation,
# verification). This script is the conversion.
#
# USAGE
#
#   ./sql/repair/normalize-collations.sh <database> [mysql arguments...]
#
#   ./sql/repair/normalize-collations.sh data_management -u qbicuser -p -h localhost
#   ./sql/repair/normalize-collations.sh qbic_usermanagement_db --defaults-file=~/.my.cnf
#
# Arguments after the database name go straight to mysql, so any credentials mechanism mysql
# understands works. Run it once per schema: it operates on DATABASE() and is not
# schema-qualified.
#
# WHY A SCRIPT AND NOT PLAIN SQL
#
#   1. Foreign keys must be dropped first. MariaDB refuses to change a column's charset while
#      it participates in a foreign key, even with FOREIGN_KEY_CHECKS=0 -- that check is
#      structural, not validation:
#         [HY000][1832] Cannot change column 'experimentId': used in a foreign key constraint
#   2. The foreign keys differ per instance, so a list copied from sql/complete-schema.sql fails:
#         [42000][1091] Can't DROP FOREIGN KEY `...`; check that it exists
#   3. Looping needs a stored procedure, and DROP PROCEDURE needs ALTER ROUTINE, which
#      application users do not have:
#         [42000][1370] alter routine command denied to user 'qbicuser'@'%'
#
# So it asks the database what needs doing, writes plain .sql files, and runs them.
#
# WHY THE TABLE LIST IS COLUMN-BASED, NOT TABLE-BASED
#
# A table can have table_collation = utf8mb4_unicode_ci while individual columns are still
# latin1 -- 'specific_measurement_metadata_ip' was exactly that. Selecting tables by
# information_schema.TABLES.TABLE_COLLATION skipped it, and the subsequent ADD CONSTRAINT then
# failed with errno 150 "Foreign key constraint is incorrectly formed". The work list is
# therefore derived from information_schema.COLUMNS.
#
# BEFORE RUNNING
#   * Back up:
#       mysqldump --single-transaction --routines --triggers --events \
#           -u <user> -h <host> <database> > before-collation-repair.sql
#   * This rebuilds every table and holds a metadata lock. Maintenance window.
#   * It is re-runnable: everything is regenerated from the live database.
#
# AFTER RUNNING
#   * Recreate the views. A view captures its columns' collations at creation time, so it keeps
#     failing until rebuilt: sql/repair/recreate-views.sql, then re-apply the two scripts in
#     sql/migrations/. An earlier run of this script dropped 18 foreign keys and restored only
#     13 because a later CREATE ... was skipped; that is why the backup below is written to a
#     persistent path and reused on a re-run.
#   * Verify with Step 5 of normalize-collations.sql.
#
set -euo pipefail

if [ $# -lt 1 ]; then
    sed -n '/^# USAGE/,/^$/p' "$0" | sed 's/^# \{0,1\}//' >&2
    exit 2
fi

DATABASE="$1"
shift
MYSQL=(mysql "$@" "$DATABASE")

BACKUP_DIR="${COLLATION_REPAIR_BACKUP_DIR:-.}"
mkdir -p "${BACKUP_DIR}"
FK_BACKUP="${BACKUP_DIR}/collation-repair-${DATABASE}-fk-backup.sql"

WORKDIR="$(mktemp -d)"
cleanup() { rm -rf "${WORKDIR}"; }
trap cleanup EXIT

echo "== instance:        ${DATABASE}"
echo "== foreign keys -> ${FK_BACKUP}"

# ---------------------------------------------------------------------------
# Generate. Everything is read from the live database, never from a file.
# ---------------------------------------------------------------------------

# The foreign keys as ready-to-run ADD statements. If a backup from an earlier run exists it is
# shown, because a run that died between drop and add has already removed some from the
# database and they can no longer be discovered.
"${MYSQL[@]}" -N -B -e "
SELECT CONCAT('ALTER TABLE \`', kcu.TABLE_NAME, '\` ADD CONSTRAINT \`', kcu.CONSTRAINT_NAME,
              '\` FOREIGN KEY (',
              GROUP_CONCAT(CONCAT('\`', kcu.COLUMN_NAME, '\`') ORDER BY kcu.ORDINAL_POSITION SEPARATOR ', '),
              ') REFERENCES ',
              IF(kcu.REFERENCED_TABLE_SCHEMA = kcu.TABLE_SCHEMA, '',
                 CONCAT('\`', kcu.REFERENCED_TABLE_SCHEMA, '\`.')),
              '\`', kcu.REFERENCED_TABLE_NAME, '\` (',
              GROUP_CONCAT(CONCAT('\`', kcu.REFERENCED_COLUMN_NAME, '\`') ORDER BY kcu.ORDINAL_POSITION SEPARATOR ', '),
              ')',
              IF(rc.DELETE_RULE = 'RESTRICT', '', CONCAT(' ON DELETE ', rc.DELETE_RULE)),
              IF(rc.UPDATE_RULE = 'RESTRICT', '', CONCAT(' ON UPDATE ', rc.UPDATE_RULE)),
              ';')
FROM information_schema.KEY_COLUMN_USAGE kcu
         JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
              ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                  AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
WHERE kcu.TABLE_SCHEMA = DATABASE()
  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
GROUP BY kcu.TABLE_NAME, kcu.CONSTRAINT_NAME, kcu.REFERENCED_TABLE_SCHEMA,
         kcu.REFERENCED_TABLE_NAME, rc.DELETE_RULE, rc.UPDATE_RULE
ORDER BY kcu.TABLE_NAME, kcu.CONSTRAINT_NAME;" > "${WORKDIR}/live_add.sql"

if [ -s "${FK_BACKUP}" ]; then
    PREVIOUS=$(grep -c . "${FK_BACKUP}" || true)
    LIVE=$(grep -c . "${WORKDIR}/live_add.sql" || true)
    if [ "${PREVIOUS}" -gt "${LIVE}" ]; then
        echo "== an earlier run left ${PREVIOUS} definitions here but only ${LIVE} exist now:"
        echo "== using the backup, so nothing already dropped is lost."
        cp "${FK_BACKUP}" "${WORKDIR}/add_fks.sql"
    else
        cp "${WORKDIR}/live_add.sql" "${WORKDIR}/add_fks.sql"
    fi
else
    cp "${WORKDIR}/live_add.sql" "${WORKDIR}/add_fks.sql"
fi

"${MYSQL[@]}" -N -B -e "
SELECT CONCAT('ALTER TABLE \`', TABLE_NAME, '\` DROP FOREIGN KEY \`', CONSTRAINT_NAME, '\`;')
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL
GROUP BY TABLE_NAME, CONSTRAINT_NAME
ORDER BY TABLE_NAME, CONSTRAINT_NAME;" > "${WORKDIR}/drop_fks.sql"

# Column-based: a table needs converting if ANY of its character columns disagrees, even when
# its table-level collation already reads utf8mb4_unicode_ci.
"${MYSQL[@]}" -N -B -e "
SELECT CONCAT('ALTER TABLE \`', c.TABLE_NAME,
              '\` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
FROM information_schema.COLUMNS c
WHERE c.TABLE_SCHEMA = DATABASE()
  AND c.COLLATION_NAME IS NOT NULL
  AND c.COLLATION_NAME <> 'utf8mb4_unicode_ci'
  AND c.TABLE_NAME NOT LIKE 'jobrunr%'
  AND c.TABLE_NAME <> 'shedlock'
  AND c.TABLE_NAME IN (SELECT t.TABLE_NAME FROM information_schema.TABLES t
                       WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_TYPE = 'BASE TABLE')
GROUP BY c.TABLE_NAME
ORDER BY c.TABLE_NAME;" > "${WORKDIR}/convert_tables.sql"

CAPTURED_FKS=$(grep -c . "${WORKDIR}/add_fks.sql" || true)
TO_CONVERT=$(grep -c . "${WORKDIR}/convert_tables.sql" || true)

echo "== foreign keys:    ${CAPTURED_FKS}"
echo "== tables:          ${TO_CONVERT}"

# Foreign keys owned by another schema cannot be dropped from here, and converting the
# referenced table fails with 1832 until that schema is repaired too.
CROSS_SCHEMA=$("${MYSQL[@]}" -N -B -e "
SELECT CONCAT(CONSTRAINT_SCHEMA, '.', TABLE_NAME, ' (', CONSTRAINT_NAME, ') -> ',
              REFERENCED_TABLE_SCHEMA, '.', REFERENCED_TABLE_NAME)
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA = DATABASE() AND CONSTRAINT_SCHEMA <> DATABASE()
ORDER BY CONSTRAINT_SCHEMA, TABLE_NAME;" || true)
if [ -n "${CROSS_SCHEMA}" ]; then
    echo
    echo "!! foreign keys pointing into ${DATABASE} from another schema:"
    echo "${CROSS_SCHEMA}" | sed 's/^/!!   /'
    echo "!! repair those schemas in the same window, or these tables will fail with 1832."
fi

# FK-named indexes with no matching constraint: drift that predates this repair. Reported, not
# silently "fixed" -- adding constraints that were never there is a data decision.
DRIFT=$("${MYSQL[@]}" -N -B -e "
SELECT CONCAT(s.TABLE_NAME, '.', s.INDEX_NAME)
FROM information_schema.STATISTICS s
WHERE s.TABLE_SCHEMA = DATABASE()
  AND (s.INDEX_NAME LIKE 'FK%' OR s.INDEX_NAME LIKE 'fk%')
  AND NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS c
                  WHERE c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = s.TABLE_NAME
                    AND c.CONSTRAINT_NAME = s.INDEX_NAME AND c.CONSTRAINT_TYPE = 'FOREIGN KEY')
GROUP BY s.TABLE_NAME, s.INDEX_NAME ORDER BY s.TABLE_NAME, s.INDEX_NAME;" || true)
if [ -n "${DRIFT}" ]; then
    echo
    echo "!! pre-existing drift: these look like foreign keys but have no constraint."
    echo "!! They are NOT restored by this script; decide separately."
    echo "${DRIFT}" | sed 's/^/!!   /'
fi

if [ "${TO_CONVERT}" -eq 0 ]; then
    echo
    echo "== nothing to convert; every column already uses utf8mb4_unicode_ci."
    exit 0
fi

echo
echo "== tables to convert:"
sed 's/^/     /' "${WORKDIR}/convert_tables.sql"
echo

# ---------------------------------------------------------------------------
# Execute. Save the definitions before dropping anything.
# ---------------------------------------------------------------------------

cp "${WORKDIR}/add_fks.sql" "${FK_BACKUP}"

echo "== 2a. dropping ${CAPTURED_FKS} foreign keys"
"${MYSQL[@]}" < "${WORKDIR}/drop_fks.sql"

echo "== 2b. converting ${TO_CONVERT} tables"
"${MYSQL[@]}" < "${WORKDIR}/convert_tables.sql"

# --force so one unsatisfiable constraint (orphan rows from years of drift) does not skip the
# remaining ADD statements, which is how an earlier run lost four of them.
echo "== 2c. restoring ${CAPTURED_FKS} foreign keys"
"${MYSQL[@]}" --force < "${WORKDIR}/add_fks.sql" || true

# ---------------------------------------------------------------------------
# Verify.
# ---------------------------------------------------------------------------

echo
echo "== 2d. verify"

"${MYSQL[@]}" -N -B -e "
SELECT CONCAT(c.TABLE_NAME, '.', c.COLUMN_NAME, '  ', c.COLLATION_NAME)
FROM information_schema.COLUMNS c
WHERE c.TABLE_SCHEMA = DATABASE()
  AND c.COLLATION_NAME IS NOT NULL
  AND c.COLLATION_NAME <> 'utf8mb4_unicode_ci'
  AND c.TABLE_NAME NOT LIKE 'jobrunr%'
  AND c.TABLE_NAME <> 'shedlock'
  AND c.TABLE_NAME IN (SELECT t.TABLE_NAME FROM information_schema.TABLES t
                       WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_TYPE = 'BASE TABLE');" \
    > "${WORKDIR}/leftover_cols.txt" || true

if [ -s "${WORKDIR}/leftover_cols.txt" ]; then
    echo "!! columns still not utf8mb4_unicode_ci:"
    sed 's/^/!!   /' "${WORKDIR}/leftover_cols.txt"
    echo "!! re-run this script: it regenerates its work list from the live database."
else
    echo "   columns:      all utf8mb4_unicode_ci"
fi

RESTORED_FKS=$("${MYSQL[@]}" -N -B -e "
SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY';")
echo "   foreign keys: ${RESTORED_FKS} of ${CAPTURED_FKS}"

if [ "${RESTORED_FKS}" != "${CAPTURED_FKS}" ]; then
    echo "!! some ADD CONSTRAINT statements failed (orphan rows). Definitions are in"
    echo "!! ${FK_BACKUP} -- do not leave the instance like this."
    exit 1
fi

echo
echo "== done. Next:"
echo "==   mysql ... < sql/repair/recreate-views.sql"
echo "==   re-apply sql/migrations/create-project-page-read-models.sql"
echo "==   re-apply sql/migrations/create-experiment-recent-activity-views.sql"
