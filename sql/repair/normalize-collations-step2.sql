-- =============================================================================
-- INSTANCE REPAIR, STEP 2: convert the application tables to utf8mb4_unicode_ci
--
-- Companion to sql/repair/normalize-collations.sql, which holds the rationale, the
-- inspection queries, view recreation and verification.
--
-- WHY THIS SCRIPT DISCOVERS WORK INSTEAD OF LISTING IT
--
-- The first version of this file hardcoded the 31 foreign keys declared in
-- sql/complete-schema.sql. It failed on the first instance it met:
--
--   [42000][1091] Can't DROP FOREIGN KEY `FKgfrw5hlq3iy6ntf32wy0e8hr`; check that it exists
--
-- That constraint is declared in the schema but does not exist on the instance, and other
-- instances carry constraints the schema does not declare. A real instance's foreign keys
-- drift, so this script reads them from information_schema instead: it captures whatever is
-- actually there, drops it, converts, and puts it back exactly as found. Same for the table
-- list -- every base table is converted (except the library-owned ones), so tables created
-- by Hibernate that are not in the schema are covered too.
--
-- WHY THE FOREIGN KEYS COME OFF FIRST
--
-- MariaDB refuses to change the character set of a column that participates in a foreign
-- key, even with FOREIGN_KEY_CHECKS=0:
--
--   [HY000][1832] Cannot change column 'experimentId': used in a foreign key
--                 constraint 'data_management/FK25vvdiupupuwmehr3o97dh7fg'
--
-- FOREIGN_KEY_CHECKS=0 disables *validation*, not the structural check.
--
-- WHAT IT DOES
--   2a. Capture every foreign key in the current schema, as ready-to-run ADD statements.
--   2b. Drop every one of them.
--   2c. Convert every base table to utf8mb4_unicode_ci, except jobrunr_* and shedlock.
--   2d. Add every captured foreign key back.
--   2e. Verify: no table left behind, and the constraint count matches 2a.
--
-- REQUIRES ALTER ROUTINE
--
-- 2b-2d need a stored procedure to loop over the discovered statements, so this script needs
-- the ALTER ROUTINE privilege. Application database users usually do not have it:
--
--   [42000][1370] alter routine command denied to user 'qbicuser'@'%' for routine
--                 'data_management._collation_repair_drop_fks'
--
-- If that is you, use sql/repair/normalize-collations.sh instead. It asks the database the
-- same questions, writes the statements to files, and runs them as plain SQL -- no routine
-- privileges required. Either way, run Step 3 (recreate-views.sql) and re-apply the two
-- sql/migrations/ scripts afterwards.
--
-- BEFORE RUNNING
--   * Back up:  mysqldump --single-transaction --routines --triggers --events \
--                   -u <user> -h <host> <database> > before-collation-repair.sql
--   * Run it against ONE database. It is not schema-qualified: it operates on DATABASE().
--     If your tables are spread over several schemas, run it once per schema.
--   * This rebuilds every table and holds a metadata lock. Maintenance window.
--
-- AFTER RUNNING
--   * Recreate every view: sql/repair/recreate-views.sql, then re-apply the two scripts in
--     sql/migrations/. A view captures its columns' collations at creation time, so it keeps
--     failing until rebuilt.
--   * Verify with Step 5 of normalize-collations.sql.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 2a. Capture the live foreign keys.
-- -----------------------------------------------------------------------------
-- Reconstructs each constraint from information_schema, including composite key columns and
-- the ON DELETE / ON UPDATE rules, so 2d can restore it faithfully.

DROP TABLE IF EXISTS `_collation_repair_fk`;
CREATE TABLE `_collation_repair_fk`
(
    `table_name`      varchar(128) NOT NULL,
    `constraint_name` varchar(128) NOT NULL,
    `add_sql`         text         NOT NULL,
    PRIMARY KEY (`table_name`, `constraint_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO `_collation_repair_fk` (`table_name`, `constraint_name`, `add_sql`)
SELECT kcu.TABLE_NAME,
       kcu.CONSTRAINT_NAME,
       CONCAT('ALTER TABLE `', kcu.TABLE_NAME,
              '` ADD CONSTRAINT `', kcu.CONSTRAINT_NAME,
              '` FOREIGN KEY (',
              GROUP_CONCAT(CONCAT('`', kcu.COLUMN_NAME, '`')
                           ORDER BY kcu.ORDINAL_POSITION SEPARATOR ', '),
              ') REFERENCES ',
              IF(kcu.REFERENCED_TABLE_SCHEMA = kcu.TABLE_SCHEMA, '',
                 CONCAT('`', kcu.REFERENCED_TABLE_SCHEMA, '`.')),
              '`', kcu.REFERENCED_TABLE_NAME,
              '` (',
              GROUP_CONCAT(CONCAT('`', kcu.REFERENCED_COLUMN_NAME, '`')
                           ORDER BY kcu.ORDINAL_POSITION SEPARATOR ', '),
              ')',
              IF(rc.DELETE_RULE = 'RESTRICT', '', CONCAT(' ON DELETE ', rc.DELETE_RULE)),
              IF(rc.UPDATE_RULE = 'RESTRICT', '', CONCAT(' ON UPDATE ', rc.UPDATE_RULE)))
FROM information_schema.KEY_COLUMN_USAGE kcu
         JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
              ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                  AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
WHERE kcu.TABLE_SCHEMA = DATABASE()
  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
GROUP BY kcu.TABLE_NAME, kcu.CONSTRAINT_NAME, kcu.REFERENCED_TABLE_SCHEMA,
         kcu.REFERENCED_TABLE_NAME, rc.DELETE_RULE, rc.UPDATE_RULE;

-- Record this number. 2e checks it comes back.
SELECT COUNT(*) AS captured_foreign_keys FROM `_collation_repair_fk`;

-- Foreign keys that point INTO this schema from another one. These are the ones 2b cannot
-- drop from here, and converting the referenced table will fail with error 1832 until the
-- owning schema has dropped them. Run this in every schema you are repairing and reconcile:
-- either repair all of them in one maintenance window, or drop these constraints first.
SELECT kcu.CONSTRAINT_SCHEMA AS fk_schema,
       kcu.TABLE_NAME        AS fk_table,
       kcu.CONSTRAINT_NAME   AS fk_name,
       kcu.REFERENCED_TABLE_SCHEMA AS referenced_schema,
       kcu.REFERENCED_TABLE_NAME   AS referenced_table
FROM information_schema.KEY_COLUMN_USAGE kcu
WHERE kcu.REFERENCED_TABLE_SCHEMA = DATABASE()
  AND kcu.CONSTRAINT_SCHEMA <> DATABASE()
ORDER BY kcu.CONSTRAINT_SCHEMA, kcu.TABLE_NAME;


-- -----------------------------------------------------------------------------
-- 2c preview. The tables that will be converted. Review before running 2b-2d.
-- -----------------------------------------------------------------------------
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
  AND TABLE_NAME NOT LIKE 'jobrunr\_%'
  AND TABLE_NAME <> 'shedlock'
  AND TABLE_NAME NOT LIKE '\_%'
ORDER BY TABLE_NAME;


-- -----------------------------------------------------------------------------
-- 2b-2d. Drop, convert, restore.
-- -----------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE IF EXISTS `_collation_repair_drop_fks`$$
CREATE PROCEDURE `_collation_repair_drop_fks`()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table VARCHAR(128);
    DECLARE v_constraint VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT `table_name`, `constraint_name`
        FROM `_collation_repair_fk`
        ORDER BY `table_name`, `constraint_name`;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    drop_loop:
    LOOP
        FETCH cur INTO v_table, v_constraint;
        IF done = 1 THEN
            LEAVE drop_loop;
        END IF;
        SET @stmt = CONCAT('ALTER TABLE `', v_table, '` DROP FOREIGN KEY `', v_constraint, '`');
        PREPARE s FROM @stmt;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END LOOP;
    CLOSE cur;
END$$

DROP PROCEDURE IF EXISTS `_collation_repair_convert_tables`$$
CREATE PROCEDURE `_collation_repair_convert_tables`()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_TYPE = 'BASE TABLE'
          AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
          AND TABLE_NAME NOT LIKE 'jobrunr\_%'
          AND TABLE_NAME <> 'shedlock'
          AND TABLE_NAME NOT LIKE '\_%'
        ORDER BY TABLE_NAME;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    convert_loop:
    LOOP
        FETCH cur INTO v_table;
        IF done = 1 THEN
            LEAVE convert_loop;
        END IF;
        SET @stmt = CONCAT('ALTER TABLE `', v_table,
                           '` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
        PREPARE s FROM @stmt;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END LOOP;
    CLOSE cur;
END$$

DROP PROCEDURE IF EXISTS `_collation_repair_add_fks`$$
CREATE PROCEDURE `_collation_repair_add_fks`()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table VARCHAR(128);
    DECLARE v_constraint VARCHAR(128);
    DECLARE v_sql TEXT;
    DECLARE cur CURSOR FOR
        SELECT `table_name`, `constraint_name`, `add_sql`
        FROM `_collation_repair_fk`
        ORDER BY `table_name`, `constraint_name`;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    add_loop:
    LOOP
        FETCH cur INTO v_table, v_constraint, v_sql;
        IF done = 1 THEN
            LEAVE add_loop;
        END IF;
        SET @stmt = v_sql;
        PREPARE s FROM @stmt;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

CALL `_collation_repair_drop_fks`();
CALL `_collation_repair_convert_tables`();
CALL `_collation_repair_add_fks`();

DROP PROCEDURE IF EXISTS `_collation_repair_drop_fks`;
DROP PROCEDURE IF EXISTS `_collation_repair_convert_tables`;
DROP PROCEDURE IF EXISTS `_collation_repair_add_fks`;


-- -----------------------------------------------------------------------------
-- 2e. Verify.
-- -----------------------------------------------------------------------------

-- Must be empty. Any row is a table that was missed (re-run this script; it is idempotent).
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_COLLATION <> 'utf8mb4_unicode_ci'
  AND TABLE_NAME NOT LIKE 'jobrunr\_%'
  AND TABLE_NAME <> 'shedlock'
  AND TABLE_NAME NOT LIKE '\_%'
ORDER BY TABLE_NAME;

-- Must equal the number 2a printed.
SELECT COUNT(*) AS restored_foreign_keys
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND CONSTRAINT_TYPE = 'FOREIGN KEY';

-- If restored_foreign_keys is lower than captured_foreign_keys, an ADD failed above; the
-- missing constraint is listed here. Do not leave the instance in that state.
SELECT b.`table_name`, b.`constraint_name`, b.`add_sql`
FROM `_collation_repair_fk` b
WHERE NOT EXISTS (SELECT 1
                  FROM information_schema.TABLE_CONSTRAINTS t
                  WHERE t.TABLE_SCHEMA = DATABASE()
                    AND t.CONSTRAINT_TYPE = 'FOREIGN KEY'
                    AND t.TABLE_NAME = b.`table_name`
                    AND t.CONSTRAINT_NAME = b.`constraint_name`);

-- Inspect the captured definitions before dropping the scratch table, then drop it:
-- SELECT * FROM `_collation_repair_fk` ORDER BY `table_name`;
DROP TABLE `_collation_repair_fk`;
