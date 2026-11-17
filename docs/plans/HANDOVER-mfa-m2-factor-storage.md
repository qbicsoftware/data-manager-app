# Handover — MILESTONE-MFA phase M2 (factor storage)

**Written:** 2026-11-17 · **Branch:** `feat/mfa-adr-0011` (base `epic/mfa`)
**Prerequisites:** ADR [`0011-mfa-authentication-authority.md`](../adr/0011-mfa-authentication-authority.md) (accepted), M1 gate closed.

Read this after the ADR. It records what M2 actually ships, what it deliberately leaves to M3/M4, and
the two places where the milestone plan is now out of date.

---

## 1. What this slice contains

| # | Artefact | State |
|---|---|---|
| M2-1 | `sql/migrations/drafts/mfa-second-factor.sql` — `user_second_factor`, `user_recovery_code`, `user_mfa_grace` | **draft**, awaiting §12 sign-off |
| M2-2 | `identity/…/domain/model/mfa/` — `SecondFactor`, `RecoveryCode`, `MfaGrace`, `ProtectedSecret`, `MfaPolicyMode`, `MfaGate` (+ decision type) | done, 82 domain assertions |
| M2-3 | `identity-infrastructure/…/mfa/` — `JpaSecondFactorRepository`, `JpaRecoveryCodeRepository`, `JpaMfaGraceRepository` | done, 11 persistence assertions incl. one concurrency test |
| M2-4 | `MfaPolicy` configuration bean | **deferred to M4-1** — see §3 |
| M2-5 | `docs/migrations/NEXT.md` operator entry | **deferred until the DDL is approved** — see §3 |

**Nothing is wired into the login path, and nothing reads these beans yet.** That is the phase
boundary: M3 enrols, M4 challenges. The slice is a tested library plus its schema, not a behaviour
change — which is also why it is safe to merge ahead of the rollout.

## 2. The decisions M2 had to make (and why)

1. **`MfaGate` returns what to persist, not a boolean.** `GraceGranted` carries the deadline the caller
   must store. A boolean would let a caller ignore the "no window exists" case and deny, which is the
   population-wide lockout of ADR 0011 item 3. Tested from both directions in `MfaGateSpec`.
2. **Grace is first-write-wins in a single statement.** `JpaMfaGraceRepository.startIfAbsent` is an
   `INSERT … ON DUPLICATE KEY UPDATE`, so two simultaneous logins cannot both move the deadline, and
   "log in again to renew" is not expressible.
3. **A `ProtectedSecret` type instead of a `String`.** The domain cannot encrypt (ADR-0002 D1), but it can
   refuse a value that is not shaped like AES-GCM ciphertext. Passing a raw base32 seed through because
   the encryptor was not wired yet now fails at enrolment, not in an audit.
4. **No `users` columns.** Enrolment is derived from active factor rows (one source of truth), and the
   grace window lives in its own table. The `users` aggregate is untouched, so this slice cannot
   regress account creation or login.
5. **No second throttling table**, per ADR 0011 and `auth-attempt-throttling.sql`. Factor-failure
   counters go into `auth_attempt` with a new `scope` value (M5-4).

## 3. Deliberately deferred, with the trigger that ends the deferral

| Deferred | Why | Ends when |
|---|---|---|
| `MfaPolicy` config bean (`off`/`optional`/`required` + grace duration) | It would be a property nothing reads. The repo's own review standard removed exactly this kind of dead code in the R1 slice (`AuthAttemptThrottle`) | **M4-1**, the first caller of `MfaGate` |
| `SecondFactorSecretProtector` port + vault-backed adapter (new key alias `second-factor-master-key`) | A fail-fast startup bean would break every deployment where the alias is not provisioned yet, for a feature that does not run | **M3-1**, when a seed is first encrypted |
| `docs/migrations/NEXT.md` operator section | Operators must not apply an unapproved draft; the promotion checklist in the SQL file covers it | when §12 sign-off is recorded |
| `sql/complete-schema.sql` sync | same | same |

## 4. Where the milestone plan is now out of date

1. **M2-3 said "same pattern as personal access tokens".** PATs are PBKDF2-hashed one way; a TOTP seed
   must be recoverable, so that is impossible. ADR 0011 F1/F2 supersedes the wording; the carrier is
   ADR-0002 S2 (master key in the PKCS12 vault, AES-GCM blobs in the DB) under a **dedicated** alias.
2. **M2-4's "per-user override"** is the exemption ADR 0011 rejects. Posture is configuration; the only
   per-account deviation is the dated grace window. An administrator-set, **tightening-only** policy is a
   future slice (ADR 0011 item 6) and reuses the same window.

## 5. What M3 must do with this

- **M3-1 (TOTP enrolment)** implements `SecondFactorSecretProtector` and the vault alias, generating the
  seed, encrypting it, and calling `SecondFactor.enrolTotp`. Recovery-code encoding is the opposite case:
  hashed with a per-code salt, verified **without** the vault key (ADR 0011 B1) — a test must assert that
  verification still works with the master key absent.
- **M3-4 (recovery codes)** must generate a whole set and call `invalidateAllFor` in the same
  transaction as `saveAll`; a partial write leaves two live sets.
- **M4-1 (login challenge)** adds `MfaPolicy`, calls `MfaGate.decide(…)`, persists `GraceGranted` via
  `startIfAbsent`, and enforces the enrolment-only restriction of `GraceActive`. The bypass suite (M4-5)
  must assert the four grace invariants at HTTP level, not only in the unit specs.
