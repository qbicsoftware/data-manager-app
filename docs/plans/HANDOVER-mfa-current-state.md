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
   can be copied to the clipboard or downloaded as a dated text file. The first confirmed factor
   issues a recovery-code set immediately, in the same breath (M3-4/M7-2).
2. **Every authenticated page** carries the enrolment prompt (`MfaEnrolmentPrompt`, mounted in
   `DataManagerLayout`): an offer under `OPTIONAL`, a dated deadline under `REQUIRED`, nothing under
   `OFF`, and nothing once a factor exists. Dismissible per session under `OPTIONAL` only.
3. **Sign in**: password → `Second factor` challenge (or recovery-code mode) → session established,
   deep link replayed.
4. **Storage**: TOTP seeds are AES-256-GCM ciphertext (60 bytes decoded) under the vault alias
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
3. **Vaadin fields are `LAZY` by value-change mode.** Server-side reformatting only runs on blur, so
   the recovery-code grouping had to move into the browser — and it took three attempts, two of them
   silent failures on a deployed build:
   1. a server-side value-change listener: LAZY, so it only ran when the field lost focus;
   2. an inline `executeJs` install on attach with a retry loop: it never attached, and a script that
      does nothing logs nothing, so "no effect" was the only symptom;
   3. **current:** a `@JsModule` script (`frontend/javascript/recoverycodeformat.js`) **delegated on
      the `document`**, acting only on fields marked `data-recovery-code-input`. No per-element
      install, no shadow-root lookup, no attach race. Verified without a browser by
      `node datamanager-app/src/test/js/recoverycodeformat.check.mjs` (15 checks: typing, pasting,
      deleting, caret). The blur-time server formatter stays as a fallback; the encoder normalises
      again before hashing, so the stored value never depends on what the browser showed.
   Two rules that came out of this and apply to every client-side decoration in the app: **delegate on
   the document** instead of installing per element, and **let the browser own transient state**
   (§3 trap 9).
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
9. **A UI state that hides the click target must not need a timer to come back.** With push off,
   `UI.accessLater` runs on the *next request*, so the icon-switch in `CopyToClipBoardComponent`
   (copy icon → check mark, restored by `CompletableFuture` + `accessLater`) left the recovery-code
   sheet looking used up after the first click: the check stayed, and a second copy looked impossible.
   `CopyToClipboardButton` therefore keeps the copy *function* on the server (proven path) and the
   confirmation on the **client**: `frontend/javascript/copyfeedback.js` sets `data-copied` on the
   clicked button and clears it 1.6 s later, and `components/copy-action.css` swaps the two icons.
   Nothing server-side has to be undone, so nothing can get stuck; the label never changes, so the
   action stays repeatable. A hidden `role="status"` node carries the same news to screen readers
   (with a time stamp, so repeated copies are re-announced).
   Two corollaries: **confirmations are decorations, not state** — put them where they need no undoing;
   and **install client scripts by delegation on the document**, not per element on attach, which is
   the install-once race that also defeated the recovery-code field grouping (§3 trap 3).
   `CopyToClipBoardComponent` is still the right tool for an inline value (setup key, command, token);
   it is the wrong tool when the copy *is* the action.

## 4. Verification you can run

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current      # /usr/bin/java is a stub in this environment
./mvnw -o -pl identity test                             # 633 tests, incl. RFC vectors, grace invariants
./mvnw -o -pl datamanager-app -am test \
    -Dtest='Mfa*,RecoveryCode*,TwoFactor*,SecondFactor*' \
    -Dsurefire.failIfNoSpecifiedTests=false   # 76 (164 in identity, same run)
node datamanager-app/src/test/js/recoverycodeformat.check.mjs   # 15, client-side formatter
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
| 0 | **Recovery-code live grouping — closed 2026-11-20** | Works as specified: upper case, hyphens added as fillers, backspace behind a hyphen deletes the group's character, pasted codes normalise to themselves. See §3 trap 3 for the pattern (document delegation) and `node datamanager-app/src/test/js/recoverycodeformat.check.mjs` for the check that does not need a browser |
| 1 | **M4-5 bypass suite** (HTTP level) | The properties are unit-tested; the suite proves no route, session reuse or replay gets past the factor. **Needs infrastructure this workspace does not have** (see §4): plan it against a booted instance, not as a unit spec |
| 2 | **Administrative re-enrolment for *enrolled* accounts (M6-1) + ADR 0012** | The unenrolled case now has a self-service path (§7). An account that *has* a factor and lost the device **and** the codes still needs identity proofing that is not the mailbox — that is the remaining decision, and the mailbox link deliberately refuses those accounts |
| 3 | **M5** — `REQUIRED` for a cohort, audit trail (`AUTH-R-04`), challenge throttling on `auth_attempt` | Enforcement is the milestone's exit gate |
| 4 | **§12 sign-off**, then promote the migration out of `drafts/` (move file, `sql/migrations/README.md` row, `complete-schema.sql`, drop the DRAFT status) | The DDL is applied on the test instance but not approved for production |
| 5 | M4-2 WebAuthn spike (optional), M3-2 | ADR 0011 W1: optional, needs the §1a proxy prerequisites |

**Closed since the previous handover:** the one-time recovery-code sheet now offers *copy all codes* and
*download as a dated text file* (`RecoveryCodesDialog.codesAsText`, `…fileName`, guarded by
`RecoveryCodesDialogSpec`). Copy is `CopyToClipboardButton` — a real `<button>` (pointer cursor,
keyboard activation) whose copy icon turns into a check mark for a moment, set and cleared in the
browser (§3 trap 9) — and download is a plain `Anchor` + `DownloadHandler`, so neither adds
browser-side JavaScript to a break-glass screen beyond the two existing modules. Not verified visually
here (no frontend build), so the sheet still needs one look on a deployed instance.

**Settings-page copy is now a truth table (2026-11-20).** A UI review found two sentences that were
false of the page they appeared on: the enrolled summary claimed the app "is required the next time you
sign in" while the posture line said two-factor authentication is *not required at sign-in* (mode
`OFF`, where `MfaGate` returns `NotRequired` regardless of the factor), and mode `OPTIONAL` claimed "you
will be asked for a code, because this account has set up a factor" on accounts with no factor. The
sentences are now pure functions of *(mode, hasFactor, unusedCodes)* in `TwoFactorSettingsCopy`, pinned
by `TwoFactorSettingsCopySpec` (24 cases) — including "no sentence may advertise an e-mail/support
reset". Consequences: recovery codes are **disabled until a factor exists** (a set generated before
that could never be redeemed, since no factor means no challenge), the card meta no longer repeats the
card title, and each `Remove` carries the factor name as its accessible label. The review, for the
record: *the "zero gap between the sections" and "button is vertically centred" findings were
retracted* once the screenshot was actually read — do not "fix" either.

**The enrolment prompt exists (M7-2, 2026-11-20).** `MfaEnrolmentPrompt` is mounted in
`DataManagerLayout`, so it reaches every authenticated page — the gap it closes is that enrolment used
to be discoverable only by someone who already knew the settings page existed. It renders only when
`TwoFactorSettingsCopy.promptVisible(mode, hasFactor, dismissed)` says so: never under `OFF` (the page
it links to reroutes away), never for an enrolled account, never after "not now" in this session. It
is hidden on the two-factor page itself, and it names the grace deadline under `REQUIRED` when a
window is stored. **Dismissal is per session, not per account** — persisting it would need a column and
a migration for a preference not worth one; under `REQUIRED` there is no dismissal at all, because an
offer may be declined and a deadline may not be hidden. Not verified visually (no frontend build).

The enrolment dialog was cut back after review (2026-11-20): one instruction sentence, the QR, the
setup key with a **repeatable** "Copy" button, and the code field. Removed on purpose, do not re-add
without a reason: the numbered three-step instructions, the "Scan with your authenticator app" caption
under the QR, the explanation in the setup-key label, and a line stating which name the entry gets in
the app. The last one was tried to diagnose the `%3A` label bug and became noise once the label was
fixed — the name is now correct by construction (`OtpauthUri.ISSUER`), and the dialog is not the place
to document it.

Known smaller gaps: no rename field on a factor (domain supports it), no "N codes left" hint at the
challenge, no per-request rate limiting on the challenge yet, and **no enrolment step in registration
(M5-2)** — a new account still reaches the app with a password only and learns about the factor from
the prompt, not from the sign-up flow.

## 6. The required route: registration, and missing the window (2026-11-23)

Three pieces landed together, and they answer the two questions the required posture raised: *how does
a new account get a factor?* and *what happens to someone who ignored the deadline?*

**A grace window is only owed to accounts that existed when the posture was flipped.** `MfaGate` has a
fifth outcome, `EnrolmentDue`: an account with no factor that is not owed a window must enrol at its
first login. Nothing is persisted for it, so nothing can expire, and the same answer comes back on every
login until a factor exists. Eligibility is a configured instant,
`qbic.security.mfa.grace-cutoff` (unset = everyone eligible, i.e. today's behaviour) compared against
the account's registration date; it lives behind `GraceEligibility` so the login guard still asks one
question and sees no repository. Two asymmetries are deliberate: a window an operator granted is still
honoured for an ineligible account (eligibility governs granting, not revoking), and an unreadable
account record answers "eligible" — a data gap must not produce a stricter path.

**The registration path is that outcome, wired into both login entry points.** Password verified → the
authentication is withdrawn → a pending login is stored → `/login/enrol`. The page starts an enrolment
with the same service the settings page uses, verifies a code, and then shows the recovery codes
*before* the session exists, waiting for the acknowledgement. Session establishment moved into
`MfaSessionCompletion`, shared with the challenge, because the details that matter (reload the
principal, mark the session or the filter loops, honour the saved request, fall back to the context
root) drift when duplicated.

**Missing the window is no longer terminal.** A one-time link, mailed to the account's mailbox, opens the
same enrolment page. It reuses the verification-token machinery under its own purpose
(`REENROL_SECOND_FACTOR`), so single use, expiry, hashing and uniform failures are the tested ones.

> **Where the mailbox is the right proof, and where it stops.** For an account with **no factor**, the
> mailbox already controls the account — a password reset makes it yours — and the link cannot finish
> what it starts, because completing the enrolment needs a code from the device. For an account **with**
> a factor the link is refused: there it would be the cheapest way *past* the requirement. That refusal
> is the boundary to M6-1, and it is a spec, not a comment.

Every request answers identically (unknown address, malformed address, mail server down), because the
endpoint is unauthenticated and must not become an account oracle.

**Reminders now run** (`MfaReminder` + `MfaReminderService` + `MfaReminderJob`): about a day before and a
couple of hours before the deadline, stamps on the grace window making the pass idempotent. The final
window decides rather than falling through, so a job that was down for a day sends one mail and not two
minutes apart. Two nullable columns were added to `user_mfa_grace`; the ALTER and the reasoning are in
`docs/migrations/NEXT.md`.

**Not done, and known:** the enrolment page and the mailed link have **not been verified visually** (no
frontend build here); the reminder job has **not been run against a database** (unit-level only, the
missing piece is one integration test against `MfaPersistenceSpec`'s harness); and an account whose owner
cannot install an authenticator app has no way in at all — the page says so instead of offering a skip.

## 7. Governance state

- **ADR 0011**: accepted by the PO; a second ADR (recovery reset identity proofing) is proposed for M6-1.
- **Requirements**: `AUTH-*` and `FEAT-AUTH-FLOWS` and ADR **0010** exist only on `development`, not on
  `epic/mfa`. Read requirement text from `origin/development`, or reconcile the branches before M5.
- **Nothing is pushed and no PR is open.** Six phases of work sit on a local branch.
