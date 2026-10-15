#!/usr/bin/env bash
#
# INSTANCE REPAIR, STEP 2: convert the application tables to utf8mb4_unicode_ci.
#
# Companion to sql/repair/normalize-collations.sql (rationale, inspection, view recreation,
# verification). This script is the conversion.
#
# WHY A SCRIPT AND NOT PLAIN SQL
#
# Two things make a static .sql file unusable for this:
#
#   1. Foreign keys must be dropped before a column's charset can change -- MariaDB refuses
#      even with FOREIGN_KEY_CHECKS=0, because that check is structural:
#        [HY000][1832] Cannot change column 'experimentId': used in a foreign key constraint
#   2. The set of foreign keys differs per instance. A list copied from
#      sql/complete-schema.sql failed with:
#        [42000][1091] Can't DROP FOREIGN KEY `FKgfrw5hlq3iy6ntf32wy0e8hr`; check that it exists
#
# A stored procedure can loop over information_schema, but DROP PROCEDURE needs ALTER ROUTINE,
# which application database users normally do not have:
#   [42000][1370] alter routine command denied to user 'qbicuser'@'%'
#
# So this script asks the database what needs doing, writes three plain .sql files, and runs
# them in order. No routine privileges are required.
#
# USAGE
#
#   ./sql/repair/normalize-collations.sh <database> [mysql arguments...]
#
#   ./sql/repair/normalize-collations.sh data_management -u qbicuser -p -h localhost
#   ./sql/repair/normalize-collations.sh qbic_usermanagement_db --defaults-file=~/.my.cnf
#
# Arguments after the database name are passed straight to mysql, so any credentials mechanism
# mysql understands works.
#
# BEFORE RUNNING
#   * Back up:
#       mysqldump --single-transaction --routines --triggers --events \
#           -u <user> -h <host> <database> > before-collation-repair.sql
#   * This rebuilds every table and holds a metadata lock. Maintenance window.
#   * It is re-runnable: everything is regenerated from the live database, so a run that died
#     halfway can simply be repeated.
#
# AFTER RUNNING
#   * Recreate the views: sql/repair/recreate-views.sql, then re-apply the two scripts in
#     sql/migrations/. A view captures its columns' collations at creation time.
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

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

echo "== instance: ${DATABASE}"
echo "== scratch:  ${WORKDIR}"

# ---------------------------------------------------------------------------
# Generate. Everything is read from the live database, never from a file.
# ---------------------------------------------------------------------------

# Foreign keys, reconstructed as ready-to-run ADD statements. Generated BEFORE anything is
# dropped, so nothing is lost.
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
ORDER BY kcu.TABLE_NAME, kcu.CONSTRAINT_NAME;" > "${WORKDIR}/add_fks.sql"

"${MYSQL[@]}" -N -B -e "
SELECT CONCAT('ALTER TABLE \`', TABLE_NAME, '\` DROP FOREIGN KEY \`', CONSTRAINT_NAME, '\`;')
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL
GROUP BY TABLE_NAME, CONSTRAINT_NAME
ORDER BY TABLE_NAME, CONSTRAINT_NAME;" > "${WORKDIR}/drop_fks.sql"

"${MYSQL[@]}" -N -B -e "
SELECT CONCAT('ALTER TABLE \`', TABLE_NAME,
              '\` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
  AND TABLE_NAME NOT LIKE 'jobrunr%'
  AND TABLE_NAME <> 'shedlock'
ORDER BY TABLE_NAME;" > "${WORKDIR}/convert_tables.sql"

CAPTURED_FKS=$(grep -c . "${WORKDIR}/add_fks.sql" || true)
TO_CONVERT=$(grep -c . "${WORKDIR}/convert_tables.sql" || true)

echo
echo "== foreign keys found:    ${CAPTURED_FKS}"
echo "== tables to convert:     ${TO_CONVERT}"

# A foreign key owned by another schema cannot be dropped from here, and converting the
# referenced table will fail until that schema is repaired too. Report, then continue: the
# failure is loud and this script is safe to re-run.
CROSS_SCHEMA=$("${MYSQL[@]}" -N -B -e "
SELECT CONCAT(CONSTRAINT_SCHEMA, '.', TABLE_NAME, ' (', CONSTRAINT_NAME, ') -> ',
              REFERENCED_TABLE_SCHEMA, '.', REFERENCED_TABLE_NAME)
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA = DATABASE()
  AND CONSTRAINT_SCHEMA <> DATABASE()
ORDER BY CONSTRAINT_SCHEMA, TABLE_NAME;" || true)

if [ -n "${CROSS_SCHEMA}" ]; then
    echo
    echo "!! Foreign keys pointing into ${DATABASE} from another schema:"
    echo "${CROSS_SCHEMA}" | sed 's/^/!!   /'
    echo "!! Those schemas must be repaired in the same window, or these constraints dropped"
    echo "!! by a user who can modify them. Otherwise conversion fails with error 1832."
    echo
fi

if [ "${TO_CONVERT}" -eq 0 ]; then
    echo
    echo "== nothing to convert; every table already uses utf8mb4_unicode_ci."
    exit 0
fi

echo "== generated files, for review:"
echo "     ${WORKDIR}/drop_fks.sql          (${CAPTURED_FKS} statements)"
echo "     ${WORKDIR}/convert_tables.sql    (${TO_CONVERT} statements)"
echo "     ${WORKDIR}/add_fks.sql           (${CAPTURED_FKS} statements)"
echo
sed 's/^/     /' "${WORKDIR}/convert_tables.sql"
echo

# ---------------------------------------------------------------------------
# Execute.
# ---------------------------------------------------------------------------

echo "== 2a. dropping ${CAPTURED_FKS} foreign keys"
"${MYSQL[@]}" < "${WORKDIR}/drop_fks.sql"

echo "== 2b. converting ${TO_CONVERT} tables"
"${MYSQL[@]}" < "${WORKDIR}/convert_tables.sql"

echo "== 2c. restoring ${CAPTURED_FKS} foreign keys"
"${MYSQL[@]}" < "${WORKDIR}/add_fks.sql"

# ---------------------------------------------------------------------------
# Verify.
# ---------------------------------------------------------------------------

echo
echo "== 2d. verify"

"${MYSQL[@]}" -N -B -e "
SELECT CONCAT(TABLE_NAME, '  ', TABLE_COLLATION)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
  AND TABLE_NAME NOT LIKE 'jobrunr%'
  AND TABLE_NAME <> 'shedlock';" > "${WORKDIR}/leftover.txt" || true

if [ -s "${WORKDIR}/leftover.txt" ]; then
    echo "!! tables still not utf8mb4_unicode_ci:"
    sed 's/^/!!   /' "${WORKDIR}/leftover.txt"
    echo "!! re-run this script: it regenerates its work list from the live database."
else
    echo "   tables:      all converted"
fi

RESTORED_FKS=$("${MYSQL[@]}" -N -B -e "
SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY';")

echo "   foreign keys: ${RESTORED_FKS} of ${CAPTURED_FKS}"

if [ "${RESTORED_FKS}" != "${CAPTURED_FKS}" ]; then
    echo "!! an ADD CONSTRAINT failed -- do not leave the instance like this."
    echo "!! the definitions are in ${WORKDIR}/add_fks.sql (kept until this script exits)"
    exit 1
fi

echo
echo "== done. Next: recreate the views (sql/repair/recreate-views.sql), then re-apply"
echo "== sql/migrations/create-project-page-read-models.sql and"
echo "== sql/migrations/create-experiment-recent-activity-views.sql."
