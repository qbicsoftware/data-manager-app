# Handover — MILESTONE-MFA, current state

**Written:** 2026-11-19 · **Branch:** `feat/mfa-adr-0011` (base `epic/mfa`, 29 commits, nothing pushed)
**Read next:** [ADR 0011](../adr/0011-mfa-authentication-authority.md) → this file → the phase handovers
([M2](HANDOVER-mfa-m2-factor-storage.md), [M3](HANDOVER-mfa-m3-totp-enrolment.md),
[M4-1](HANDOVER-mfa-m4-1-login-challenge.md)) → [milestone plan](MILESTONE-MFA-self-governed-2fa.md).

---

## 1. What works end-to-end on a running build

Verified on a deployed instance (context path `/dev`, `qbic.security.mfa.mode=OPTIONAL`):

1. **Settings → Two-factor authentication**: enrol an authenticator app by scanning a QR or typing the
   setup key, confirm with a code, see/remove factors, generate a one-time recovery-code sheet that
   can be copied to the clipboard or downloaded as a dated text file.
2. **Sign in**: password → `Second factor` challenge (or recovery-code mode) → session established,
   deep link replayed.
3. **Storage**: TOTP seeds are AES-256-GCM ciphertext (60 bytes decoded) under the vault alias
   `second-factor-master-key`; recovery codes are HMAC-SHA-256 hashes under `RECOVERY_CODE_PEPPER`.

Nothing is enforced until `qbic.security.mfa.mode` is set to `OPTIONAL`/`REQUIRED` (default `OFF`).

## 2. Where the code lives

| Concern | Location |
|---|---|
| Domain: factors, recovery codes, grace, gate, TOTP, Base32, otpauth | `identity/…/domain/model/mfa/` |
| Application: policy, guard, verification, enrolment, recovery service | `identity/…/application/mfa/` |
| Ports | `identity/…/domain/repository/{SecondFactor,RecoveryCode,MfaGrace}Repository` |
| JPA adapters | `identity-infrastructure/…/mfa/` |
| Challenge, handler, enforcement filter, session state | `datamanager-app/…/security/mfa/` |
| Settings page + dialogs, QR component + JS | `datamanager-app/…/views/account/`, `frontend/javascript/totpqrcode.js` |
| DDL (draft, §12 pending) | `sql/migrations/drafts/mfa-second-factor.sql` |
| Runbook, provisioning | `docs/migrations/NEXT.md` §"Migration 1"; `docs/operations/installation-and-administration.md` |

## 3. Traps already paid for — do not rediscover these

1. **A framework configurer can replace `formLogin().successHandler(...)`.** It happened: policy
   `OPTIONAL`, account enrolled, no challenge, and *no* handler log line. Enforcement therefore lives in
   `MfaChallengeEnforcementFilter`, which runs on every request and cannot be configured away. The
   success handler is only an optimisation.
2. **Spring saves the security context before the success handler.** Withholding a password-only
   session means clearing `SecurityContextHolder` **and**
   `HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY`.
3. **Vaadin fields are `LAZY` by value-change mode.** Server-side reformatting only runs on blur; the
   recovery-code grouping is therefore client-side. **It still does not work on the deployed build**
   (typed text stays raw, e.g. `dsadsadasdsa`): the first version executed from the constructor, where
   the element has no shadow root yet, so `shadowRoot.querySelector('input')` was null and the
   install-once script silently did nothing. The current version installs on attach and retries until
   the input exists — **not yet verified on a running build.** If it still fails, the radio silence is
   the tell: nothing logs, so "no effect" means the listener never attached. Fallbacks in order:
   (a) a proper `@JsModule` client-side script next to `totpqrcode.js`, (b) `ValueChangeMode.EAGER`
   with server-side formatting (works, but round-trips per keystroke and jumps the caret).
4. **Any service that consumes a code needs `@Transactional`** (pessimistic lock + flush otherwise throw
   `No active transaction`). A reflection assertion in `RecoveryCodeServiceSpec` guards this.
5. **Redirects need `request.getContextPath()`**: a hard-coded `/` lands outside `/dev`.
6. **`QbicUserDetails.getUsername()` returns the user id**, not the email. The pending login takes the
   email from `getEmailAddress()`.
7. **The DDL must align FK columns with `users.id` at apply time** (this deployment is
   `utf8mb3_german2_ci`) and should refuse a wrong schema — both are in the script now. `users` alone
   does not identify the data-management database; `projects_datamanager` does.
8. **Helpdesk reset is an identity-proofing bypass.** Accounts are self-service + email-verified, so an
   advertised email reset would make mailbox control the way past the factor. The challenge says a reset
   cannot be granted by email. See §5.

## 4. Verification you can run

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current      # /usr/bin/java is a stub in this environment
./mvnw -o -pl identity test                             # 633 tests, incl. RFC vectors, grace invariants
./mvnw -o -pl datamanager-app -am test \
    -Dtest='Mfa*,RecoveryCode*,TwoFactor*,SecondFactor*' \
    -Dsurefire.failIfNoSpecifiedTests=false   # 76 (164 in identity, same run)
```

The Vaadin frontend build **cannot** run here (`~/.vaadin` is read-only), so UI changes are
compile-verified only; run `./mvnw -pl datamanager-app -am package` elsewhere. The persistence spec runs
the real draft DDL against MariaDB with `-Dmfa.jdbc.url=… -Dmfa.dialect=org.hibernate.dialect.MariaDBDialect`.

Two facts about this suite that cost time to find out:

- **There is no `@SpringBootTest` in `datamanager-app`, by design.** A full context needs MariaDB,
  Artemis, an SMTP host, an OpenBIS instance and the vault (see `SecurityConfigurationBeanCycleSpec`),
  so the M4-5 HTTP-level bypass suite cannot be built *or run* in this workspace. It needs a machine
  with that infrastructure; anything written blind here would be unverified test code, which is worse
  than a recorded gap.
- **A spec with a hard-coded `Instant` is a time bomb.** `MfaLoginSuccessHandlerSpec` fixed its
  instants at `2026-11-18T09:00:00Z` while the handler asks the guard with `Instant.now()`; the
  "running grace window" case started failing on 2026-11-20T08:00Z, two days later, with no code
  change. Fixed by anchoring the grace windows at `Instant.now()` (2026-11-20). When a spec cannot
  inject a clock into the code under test, derive its instants from the real clock instead of pinning
  them.

## 5. Open work, in the order I would do it

| # | Task | Why it is next |
|---|---|---|
| 0 | **Recovery-code live grouping (open, parked)** | Two attempts failed and the broken inline script is removed (see §3 trap 3). Needs a `@JsModule` client script plus browser-console verification **before** coding further. Until then the field formats on blur only; submission is still correct. Do not re-attempt blind: the last two iterations cost deployment round trips and gave no signal |
| 1 | **M4-5 bypass suite** (HTTP level) | The properties are unit-tested; the suite proves no route, session reuse or replay gets past the factor. **Needs infrastructure this workspace does not have** (see §4): plan it against a booted instance, not as a unit spec |
| 2 | **M6-1 administrative re-enrolment + ADR 0012** | The challenge's support copy promises identity verification; that needs a process decision (proofing that is not the mailbox, four-eyes, audit) before `REQUIRED` |
| 3 | **M5** — `REQUIRED` for a cohort, audit trail (`AUTH-R-04`), challenge throttling on `auth_attempt` | Enforcement is the milestone's exit gate |
| 4 | **§12 sign-off**, then promote the migration out of `drafts/` (move file, `sql/migrations/README.md` row, `complete-schema.sql`, drop the DRAFT status) | The DDL is applied on the test instance but not approved for production |
| 5 | M4-2 WebAuthn spike (optional), M3-2 | ADR 0011 W1: optional, needs the §1a proxy prerequisites |

**Closed since the previous handover:** the one-time recovery-code sheet now offers *copy all codes* and
*download as a dated text file* (`RecoveryCodesDialog.codesAsText`, `…fileName`, guarded by
`RecoveryCodesDialogSpec`). The copy path reuses `CopyToClipBoardComponent` — the only clipboard
mechanism already proven on a running build — and the download is a plain `Anchor` + `DownloadHandler`,
so neither adds browser-side JavaScript to a break-glass screen. Not verified visually here (no
frontend build), so the sheet still needs one look on a deployed instance.

Known smaller gaps: no rename field on a factor (domain supports it), no "N codes left" hint at the
challenge, no per-request rate limiting on the challenge yet.

## 6. Governance state

- **ADR 0011**: accepted by the PO; a second ADR (recovery reset identity proofing) is proposed for M6-1.
- **Requirements**: `AUTH-*` and `FEAT-AUTH-FLOWS` and ADR **0010** exist only on `development`, not on
  `epic/mfa`. Read requirement text from `origin/development`, or reconcile the branches before M5.
- **Nothing is pushed and no PR is open.** Six phases of work sit on a local branch.
