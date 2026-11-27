-- =============================================================================
-- Migration: WebAuthn credential lookup key and signature counter
-- Story:    MILESTONE-MFA M3-2 (WebAuthn as an offered factor type)
-- ADRs:     0011 (MFA authentication authority: the application owns the second factor)
-- Datasource: data_management
--
-- Status:   SAFE TO APPLY to a live instance, and idempotent. Two additive columns
--           and one unique key on an existing table. No row is read, rewritten or
--           deleted, no existing column changes type, and every statement is guarded
--           on information_schema so re-running is a no-op.
--
-- Why this file exists: `mfa-second-factor.sql` was already applied to a testing
--           environment before these two columns were added to it. The draft now
--           creates them for a fresh install; this brings an existing install to the
--           same shape without a drop-and-recreate.
--
-- Purpose: give a WebAuthn credential the two things that cannot live in the opaque
--          `webauthnCredential` blob.
--
--   credentialId   the LOOKUP KEY. A WebAuthn assertion arrives carrying the credential id
--                  and nothing that names an account - proving possession is the whole
--                  point of the protocol. The login path must therefore resolve the factor
--                  from the credential id alone, which needs an indexed lookup, not a scan
--                  of opaque text. This is the same reason user_recovery_code.codeHash is
--                  globally unique, and that column's comment already explains it.
--                  Sizing: credential ids are 16-1023 opaque bytes, so Base64url is
--                  22-1364 characters. varchar(1024) covers the legal range.
--
--   signatureCount the COMPARE-AND-SET counter. Authenticators keep a monotonic signature
--                  counter, and a counter that fails to advance is the only signal we get
--                  that a credential may have been cloned. Checking it requires
--                  read -> verify -> write-if-unchanged, i.e.
--                  UPDATE ... WHERE signatureCount = :expected. A read-modify-write of the
--                  text blob has no atomicity: two concurrent assertions both read the old
--                  value and both write their own, so the check always passes and detects
--                  nothing. That is why it is a column.
--                  NULL means 'this authenticator reports no counter', which WebAuthn
--                  permits. 0 means 'it reports one, and it is currently zero'. The two are
--                  NOT interchangeable, so this column is deliberately unconstrained by the
--                  shape CHECK - collapsing them would switch off clone detection for every
--                  authenticator whose counter starts at zero.
--
-- Rollback (drops the new state, not any TOTP data):
--   DROP INDEX uk_user_second_factor_credential ON user_second_factor;
--   ALTER TABLE user_second_factor DROP COLUMN signatureCount;
--   ALTER TABLE user_second_factor DROP COLUMN credentialId;
--   (and restore chk_user_second_factor_shape to its previous definition, printed in
--    step 4 below before it is replaced)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Pre-flight: right schema, right table, and what is already in it
-- -----------------------------------------------------------------------------
SELECT IF(
    (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'user_second_factor') = 1,
    'Pre-flight OK: user_second_factor found',
    'STOP: user_second_factor not found in this schema. Connect to the data-management database (see DATAMANAGEMENT_DB_URL) before running this.'
) AS preflight_table;

-- Existing factors by type. Reported, not acted on: this script must not delete rows.
SELECT type, COUNT(*) AS factors,
       SUM(CASE WHEN disabledAt IS NULL THEN 1 ELSE 0 END) AS active
  FROM user_second_factor
 GROUP BY type;

-- If any WEBAUTHN rows exist, step 5 will refuse to widen the CHECK over them
-- (a WebAuthn row without a credential id can never be resolved at login). Report
-- them now so the operator sees it before the DDL, not after.
SELECT type, COUNT(*) AS rows_
  FROM user_second_factor
 WHERE type = 'WEBAUTHN'
 GROUP BY type;

-- -----------------------------------------------------------------------------
-- 2. credentialId
-- -----------------------------------------------------------------------------
SET @has_credential_id = (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'user_second_factor'
     AND column_name = 'credentialId');

SET @ddl = IF(@has_credential_id = 0,
  'ALTER TABLE user_second_factor ADD COLUMN credentialId varchar(1024) DEFAULT NULL COMMENT ''WEBAUTHN only. Base64url credential id. Globally unique: the assertion path looks a credential up by this value WITHOUT knowing the account. Never a secret - it is public, signed material, sent to the browser on every challenge.''',
  'SELECT ''credentialId already present - nothing to do'' AS noop');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. signatureCount
-- -----------------------------------------------------------------------------
SET @has_signature_count = (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'user_second_factor'
     AND column_name = 'signatureCount');

SET @ddl = IF(@has_signature_count = 0,
  'ALTER TABLE user_second_factor ADD COLUMN signatureCount bigint(20) DEFAULT NULL COMMENT ''WEBAUTHN only. Signature counter of the last accepted assertion, compared-and-set on every use so a frozen or rewound counter is detectable as a possible clone. NULL = the authenticator reports no counter (permitted by WebAuthn); 0 = it reports one and the value is currently zero. Not interchangeable.''',
  'SELECT ''signatureCount already present - nothing to do'' AS noop');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 4. The unique key on credentialId
-- -----------------------------------------------------------------------------
-- NOT unique-per-user, globally unique: the assertion path resolves a credential
-- without knowing whose it is, so the id is the lookup key and two users must never
-- share one. MySQL and MariaDB both allow any number of NULLs in a UNIQUE index, so
-- this is safe to add while every existing TOTP row holds NULL.
SET @has_uk = (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'user_second_factor'
     AND index_name = 'uk_user_second_factor_credential');

SET @ddl = IF(@has_uk = 0,
  'ALTER TABLE user_second_factor ADD UNIQUE KEY uk_user_second_factor_credential (credentialId)',
  'SELECT ''uk_user_second_factor_credential already present - nothing to do'' AS noop');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 5. Widen the shape CHECK to cover the new columns  --  OPTIONAL, guarded
-- -----------------------------------------------------------------------------
-- The existing CHECK is an OR over both factor shapes, but it predates these
-- columns, so on its own it would let a TOTP row carry a credentialId and a WEBAUTHN
-- row have none. Widening it is the other half of 'do this while the table is small'.
--
-- Why it is guarded rather than unconditional: the tightened predicate requires a
-- WEBAUTHN row to HAVE a credentialId, so it cannot be added over rows that lack one.
-- Those rows cannot be real passkeys anyway - without an id nothing can ever look them
-- up - so they are test artefacts. The block therefore acts only when it is safe, and
-- says so loudly when it is not. Re-run this script after cleaning them up.
SET @unsafe_webauthn = (
  SELECT COUNT(*) FROM user_second_factor
   WHERE type = 'WEBAUTHN' AND credentialId IS NULL);

-- Print the current definition first, so the rollback in the header is concrete.
SELECT constraint_name, check_clause AS current_definition
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE()
   AND constraint_name = 'chk_user_second_factor_shape';

SET @ddl = IF(@unsafe_webauthn > 0,
  CONCAT('SELECT ''SKIPPED step 5: ', @unsafe_webauthn,
         ' WEBAUTHN row(s) have no credentialId. Those cannot be resolved at login, so they are test rows - delete them and re-run to widen the CHECK.'' AS notice'),
  IF((SELECT COUNT(*) FROM information_schema.check_constraints
       WHERE constraint_schema = DATABASE()
         AND constraint_name = 'chk_user_second_factor_shape') = 0,
    'SELECT ''SKIPPED step 5: chk_user_second_factor_shape not present - the table was created by Hibernate, not by the draft. Add the CHECK manually if you want it.'' AS notice',
    -- DROP CHECK is MySQL 8 syntax; MariaDB wants DROP CONSTRAINT (10.2.22+). The
    -- server name is in @@version on MariaDB, so pick the keyword rather than shipping
    -- a script that fails on one of the two engines.
    CONCAT('ALTER TABLE user_second_factor DROP ',
           IF(@@version LIKE '%MariaDB%', 'CONSTRAINT', 'CHECK'),
           ' chk_user_second_factor_shape, ADD CONSTRAINT chk_user_second_factor_shape CHECK ((type = ''TOTP'' AND secretCiphertext IS NOT NULL AND webauthnCredential IS NULL AND credentialId IS NULL AND signatureCount IS NULL) OR (type = ''WEBAUTHN'' AND webauthnCredential IS NOT NULL AND secretCiphertext IS NULL AND credentialId IS NOT NULL))')));

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 6. Verify
-- -----------------------------------------------------------------------------
SELECT column_name, column_type, is_nullable, column_comment
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'user_second_factor'
   AND column_name IN ('credentialId', 'signatureCount')
 ORDER BY column_name;
-- Expect exactly two rows: credentialId varchar(1024) YES, signatureCount bigint(20) YES.

SELECT index_name, non_unique, column_name
  FROM information_schema.statistics
 WHERE table_schema = DATABASE()
   AND table_name = 'user_second_factor'
   AND index_name = 'uk_user_second_factor_credential';
-- Expect exactly one row: non_unique = 0, column_name = credentialId.

SELECT constraint_name, check_clause AS definition_after
  FROM information_schema.check_constraints
 WHERE constraint_schema = DATABASE()
   AND constraint_name = 'chk_user_second_factor_shape';
-- Expect the widened predicate, unless step 5 reported SKIPPED.

-- -----------------------------------------------------------------------------
-- 7. Behavioural probe  --  run in a transaction, expected to fail, roll back
-- -----------------------------------------------------------------------------
-- Proves the CHECK is live rather than merely present. Commented out because it is
-- meant to raise an error; uncomment to run it deliberately.
--
-- START TRANSACTION;
-- INSERT INTO user_second_factor (userId, type, displayName, createdAt)
-- VALUES ('probe', 'WEBAUTHN', 'probe', NOW(6));   -- must fail: no credentialId
-- ROLLBACK;

-- No backfill is needed or wanted. NULL is the correct state for both columns on
-- every existing row: a TOTP factor has no credential id and no counter, and there is
-- no WebAuthn factor whose counter has been observed yet.

-- =============================================================================
-- What the application does with a NULL counter
-- =============================================================================
-- Nothing yet: the JPA entity deliberately does not map these two columns, so this
-- script is forward preparation. Mapping them changes the INSERT Hibernate generates,
-- which is Phase 2 of docs/plans/PLAN-mfa-webauthn-fido2.md. Applying this early is
-- harmless precisely because no code reads the columns.

-- =============================================================================
-- Tested against
-- =============================================================================
-- H2 2.5.252 in MODE=MySQL, on a schema created from the pre-change draft with one
-- TOTP factor already enrolled. Verified:
--
--   * the existing row survives the upgrade with both columns NULL;
--   * the three ALTERs and the CHECK replacement all apply cleanly;
--   * a new TOTP row is still accepted;
--   * a TOTP row carrying a credentialId is refused;
--   * a WEBAUTHN row without a credentialId is refused;
--   * a WEBAUTHN row with one is accepted, including signatureCount = 0;
--   * a second row reusing the same credentialId is refused.
--
-- NOT executed against MariaDB itself: no server could be started in the authoring
-- environment. The dialect-sensitive parts are the DROP CONSTRAINT keyword (handled via
-- @@version, see step 5) and PREPARE/EXECUTE, both of which the sibling script
-- add-second-factor-accepted-step.sql already uses on a live instance. Run this on the
-- test database and keep the step-6 output before promoting it.
