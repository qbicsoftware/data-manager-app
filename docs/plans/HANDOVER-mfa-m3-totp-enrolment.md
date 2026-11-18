# Handover — MILESTONE-MFA phase M3-1 (TOTP enrolment)

**Written:** 2026-11-17 · **Branch:** `feat/mfa-adr-0011` (base `epic/mfa`)
**Prerequisites:** ADR [0011](../adr/0011-mfa-authentication-authority.md) (accepted), [M2 handover](HANDOVER-mfa-m2-factor-storage.md) (factor store + grace gate).

---

## 1. What this slice contains

| Part | Artefact | Test evidence |
|---|---|---|
| Algorithm | `identity/…/domain/model/mfa/Totp.java`, `Base32.java`, `OtpauthUri.java` | 75 assertions, **RFC 4226/4648 vectors**, ±1-step skew, malformed input |
| Enrolment | `identity/…/application/mfa/TotpEnrolmentService.java`, `PendingTotpEnrolment` | 13 assertions: begin persists nothing, wrong code creates nothing, abandoned enrolment expires |
| Secret protection | `SecondFactorSecretProtector` port + `AesGcmSecondFactorSecretProtector` + `MfaConfiguration` | 10 assertions: round trip, fresh nonce, tamper/wrong-key/missing-key all fail closed |
| Config | `qbic.security.vault.second-factor.key-alias` (`application.properties`, template) | — |

| Recovery codes | `RecoveryCodes`, `RecoveryCodeEncoder`, `RecoveryCodeService` (M3-4) | 43 assertions + a JPA round trip |
| Settings page | `views/account/TwoFactorAuthentication{Main,Component}`, `TotpEnrolmentDialog`, `RecoveryCodesDialog`, `TotpQrCode` + `frontend/javascript/totpqrcode.js`, nav tab (M3-3) | compiles; **no automated UI test** (views are excluded from coverage in this repo) |

The journey is now reachable in a browser: **Settings → Two-factor authentication**. Nothing is wired
into the *login* path yet — that is M4, and until then an enrolled factor is simply not asked for.

## 2. Decisions worth knowing

1. **No new crypto, and no new dependency.** `AesGcmSecondFactorSecretProtector` wraps the reviewed
   `AesGcmCredentialEncryptor` (ADR-0002) with a *different key*. The adapter lives in the composition
   root (`datamanager-app`), because putting it in `identity-infrastructure` would force the identity
   context to depend on the project-management one.
2. **The vault key is resolved lazily, not at startup.** A fail-fast bean would stop every deployment
   where `second-factor-master-key` is not yet provisioned — all of them today. The key is read on
   first use; failure surfaces as `SecondFactorSecretUnavailableException` (fail closed, AUTH-NFR-02).
   **M4-1 should make it eager** when the login path depends on factors, and the release note must say
   to provision the alias first.
3. **TOTP parameters are fixed** (SHA-1, 6 digits, 30 s, ±1 step). Not configurable on purpose: the
   strength comes from the 160-bit seed, and each extra tolerated step widens the relay window.
4. **A code is not single-use in the algorithm.** It stays valid for its whole 30-second step; replay
   protection is the login step's job (M4), which must remember the accepted step per account.
5. **Enrolment window: 15 minutes.** A started enrolment that is never confirmed must go stale; it is
   cheap to restart and leaves no row behind.

## 3. Operator action before the first deployment that enrols a factor

Provision the alias in the PKCS12 keystore (same procedure as
[`released/v1.16.0.md`](../migrations/released/v1.16.0.md) §key provisioning for the external-credential
key), with a **fresh** 32-byte Base64 AES key:

```bash
# 32 random bytes, Base64-encoded, imported under the second-factor alias
head -c 32 /dev/urandom | base64 | keytool -importpass \
  -alias second-factor-master-key -keystore keystore.p12 -storetype PKCS12 ...
```

Record the key in the backup/restore runbook: losing it does not lock users out (recovery codes are
hashed outside this path, ADR 0011 B1) but it does invalidate every TOTP factor.

## 4. What remains in M3

| Task | Depends on | Note |
|---|---|---|
| M3-3 settings surface | **done 2026-11-17** | `/settings/two-factor`: enrol (setup key + copy, code confirmation), factor list with remove, recovery-code generation with the one-time sheet. See the gaps below |
| M3-2 WebAuthn spike | — | optional per ADR 0011 W1; needs §1a prerequisites |

### M3-4 recovery codes — done (2026-11-17)

Ten codes per set, 12 Base32 characters each (60 bits), printed as `XXXX-XXXX-XXXX`. Generation returns
the plaintext set **once** and stores HMAC-SHA-256 hashes; regeneration retires the previous set in the
same transaction. Redemption needs no account id: the hash *is* the lookup key.

Two decisions worth keeping:

- **Keyed hash, not a salted one.** A per-code salt would be stronger against offline guessing but makes
  the hash non-indexable, and the recovery path has no account id to scope by. The pepper provides the
  "a database dump is not enough" property instead.
- **The pepper is configuration, not the vault key** (`qbic.security.recovery-code.pepper`, env
  `RECOVERY_CODE_PEPPER`). That is what keeps ADR 0011 B1 true: a lost vault key must not take the
  recovery codes with it. It must be backed up with the config; a rotated pepper invalidates every
  outstanding code.

## 4a. M3-3 gaps, stated rather than hidden

1. ~~No QR image.~~ **Closed 2026-11-18:** the dialog renders a scannable QR (`TotpQrCode`), drawn in
   the browser with the npm package `qrcode` via `@NpmPackage`, bundled at build time like
   `cropperjs`. The setup key stays below it as the fallback for cameras that will not cooperate.
   A server-side encoder (ZXing) was tried first and rejected by the build's PGP-signature gate: its
   key is not in the trusted map, and `zxing:javase` drags in an unmaintained `jai-imageio-core`. That
   would have meant widening a security allow-list to draw a QR code.')
2. **No rename in the UI.** `SecondFactor.rename` exists and is tested; a label field in the factor card
   is a small follow-up.
3. **No WebAuthn entry point.** Correct while M3-2 has not shipped (ADR 0011 W1).
4. **No policy banner.** The mode (off/optional/required) arrives with M4; a countdown banner before
   then would announce a deadline nothing enforces.

## 5. Open issue / drift reminder

`AUTH-*` requirements, `FEAT-AUTH-FLOWS` in `docs/features.md` and ADR **0010** exist only on
`development`, not on `epic/mfa`. Any slice that cites requirement text should read it from
`origin/development`, or the branches should be reconciled before M4.
