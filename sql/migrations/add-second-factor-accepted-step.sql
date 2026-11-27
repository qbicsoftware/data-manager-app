-- =============================================================================
-- Migration: TOTP replay protection — record the highest accepted time step
-- Story:    MILESTONE-MFA M4-1 follow-up (QA finding MFA-20)
-- ADRs:     0011 (MFA authentication authority: the application owns the second factor)
-- Datasource: data_management
--
-- Status:   SAFE TO APPLY to a live instance. One additive column on an existing
--           table: no row is read, rewritten or deleted, no constraint is added,
--           and no existing column changes type. Existing rows get NULL, which the
--           application reads as "no code accepted yet" - correct for every row.
--
-- Purpose: refuse a TOTP code that has already been used. A code is valid for its
-- 30-second step and verification tolerates one step either side for clock drift,
-- so a code that has been seen once stays usable for ninety seconds and any number
-- of times within it. lastAcceptedStep records the highest RFC 6238 time step the
-- factor has accepted; a code whose step is not strictly newer is refused.
--
-- Why a step counter and not a timestamp: every datetime column in this schema is
-- datetime(6) with no zone, so a timestamp would inherit the database session's
-- timezone and could disagree with the application by the offset. A step number is
-- the value fed into the HMAC counter - it IS the identity of the 30-second window,
-- so there is nothing to convert and nothing to get wrong. It also needs no sweeper:
-- it only ever moves forward.
--
-- Idempotent: guarded on information_schema, so re-running is a no-op.
--
-- Rollback (drops the protection, not any data):
--   ALTER TABLE user_second_factor DROP COLUMN lastAcceptedStep;
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Pre-flight: this must be the data-management schema
-- -----------------------------------------------------------------------------
SELECT IF(
    (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'user_second_factor') = 1,
    'Pre-flight OK: user_second_factor found',
    'STOP: user_second_factor not found in this schema. Connect to the data-management database (see DATAMANAGEMENT_DB_URL) before running this.'
) AS preflight_second_factor;

-- -----------------------------------------------------------------------------
-- The column, added only if it is missing
-- -----------------------------------------------------------------------------
SET @has_column = (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'user_second_factor'
     AND column_name = 'lastAcceptedStep');

SET @ddl = IF(@has_column = 0,
  'ALTER TABLE user_second_factor ADD COLUMN lastAcceptedStep bigint(20) DEFAULT NULL COMMENT ''Highest RFC 6238 time step this factor has accepted (MFA-20 replay protection)''',
  'SELECT ''lastAcceptedStep already present - nothing to do'' AS noop');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- Verify
-- -----------------------------------------------------------------------------
SELECT column_name, column_type, is_nullable
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'user_second_factor'
   AND column_name = 'lastAcceptedStep';
-- Expect exactly one row: bigint(20), YES.

-- No backfill is needed or wanted: NULL is the correct initial state for every
-- existing factor, and the mark only has to be right from the next verification on.
SELECT COUNT(*) AS existing_factors_starting_null
  FROM user_second_factor
 WHERE lastAcceptedStep IS NULL;
