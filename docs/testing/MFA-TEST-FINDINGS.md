# MFA test findings — 2026-11-25 run

Source: `Testing MFA DataManager.docx` (environment `qpylon/dev`, branch `epic/mfa`).
97 entries, 10 recorded failures. This file is the working to-do list for `fix/mfa-feature-fixes`.

Severity key: **S1** identity compromise · **S2** missing security control · **S3** broken
functionality · **S4** user-facing defect · **S5** decide, may not need code.

## Status

| Finding | Severity | State |
|---|---|---|
| "Choose different account" authenticates as the previous account (MFA-25) | S1 | **fixed** — `710d2001f` |
| No throttling on repeated failed codes (MFA-68) | S2 | **fixed** — `2cfa52608`, completed by `f1d052997` |
| Recovery codes lost by refreshing (MFA-91) | S4 | **fixed** — `713fb4649` |
| Registration broken / confirmed account cannot log in (MFA-74, MFA-75) | S3 | open — see §3 |
| Invalid samples reported as registered (MFA-83) | S3 | open |
| No warning when the last recovery code is consumed (MFA-41b) | S4 | **fixed** — `775474246` |
| Enrolment buttons unreachable at low resolution (MFA-93) | S4 | open |
| Misleading login and MFA error messages (MFA-94) | S4 | open |
| Deep-link return after login (MFA-80) | S5 | needs a decision |
| TOTP replay accepted (MFA-20, recorded Success) | S5 | needs a decision |

---

## 1. Security: authentication identity confusion

### S1 — "Choose different account" authenticates as the previous account — **fixed**

> **Fixed in `710d2001f`.** `MfaLoginSuccessHandler` only ever added its pending login; it now
> discards both MFA session attributes at the start of every successful password authentication, so
> each login starts from a session that describes only itself. `MfaAccountSwitchSpec` reproduces the
> four-step sequence and failed three of six cases before the change.

- **Test:** MFA-25, *Cancel the MFA challenge*. Recorded FAILURE.
- **Repro:** log in as account A (TOTP configured, email + password) → at the TOTP challenge choose
  "choose different account" → log in as account B (no TOTP, email + password) → account A's TOTP
  challenge appears → entering A's TOTP logs the user in **as A**.
- **Impact:** identity substitution. The second credentials are accepted but the resulting session
  belongs to the first account. A non-enrolled account cannot complete the flow at all. The wrong
  state survives a page refresh.
- **Likely cause:** `MfaPendingLogin` / `MfaSessionState` are not cleared when a challenge is
  abandoned, so the next login attaches to the stale pending challenge.
- **Fix must include:** clear pending state on "choose different account", on logout, and on a new
  password-authentication success; a spec proving B's credentials can never yield A's session.

## 2. Security: missing control

### S2 — No throttling on repeated failed TOTP attempts — **fixed**

> **Fixed in `2cfa52608`.** The throttle machinery already existed and its `Scope` enum already named
> this flow as the next user; the scope was simply never added. `Scope.SECOND_FACTOR` now carries its
> own policy (5 per 15 min, `qbic.auth-throttle.second-factor.*`), enforced in
> `SecondFactorVerificationService` rather than the view, because the view was the only place that
> could have checked and did not. No schema change: the `auth_attempt` table takes a fourth scope.
>
> **That fix was incomplete, and reviewing it found two more defects (`f1d052997`).**
>
> 1. **The limit was on the wrong secret.** Only the authenticator code was guarded;
>    `RecoveryCodeService.redeemFor` — the sole recovery-code path on the challenge — was not. So the
>    six-digit code that rotates every thirty seconds was limited, and the twelve-character code that
>    stays valid until redeemed was not. Both now share **one** bucket, because two would let an
>    attacker spend five of each per window against the same login.
> 2. **The user was never told.** The silent refusal reused the uniform "not accepted" sentence, on
>    reasoning that belongs to the anonymous password login. Reaching this limit requires the account's
>    password, so it protects nothing and leaves a real user typing valid codes into a page that keeps
>    calling them wrong. The challenge now states the limit, the wait, and that the password and app
>    still work — and disables the controls.
>
> **Two follow-ups from the same review (`S3` on the branch, reported live).**
>
> 3. **A refresh handed back a clean form to a blocked account.** The block was never in doubt — a
>    retry cannot move it — but `beforeEnter` only checked the pending login, so the page rendered
>    its normal state and the user tried again immediately. `AuthAttemptThrottle.remainingBlock` is a
>    read-only peek; `beforeEnter` uses it to re-enter the blocked state, and it reports what is
>    *left` rather than the configured maximum. Reading never records an attempt, so reloading cannot
>    extend a block — pinned by `RemainingBlockSpec` and `SecondFactorRefreshWhileBlockedSpec`.
> 4. **The message was too long to be read.** Cut to one sentence: "Too many failed attempts. You can
>    try again in 15 minutes." The explanatory tail was true and buried the only fact the user wanted.

- **Test:** MFA-68. Recorded FAILURE.
- **Repro:** submit wrong codes repeatedly (~2 min of clicking) — nothing slows, blocks or locks.
- **Open question:** the implementer is unsure whether throttling exists at all; grep for a
  rate limiter on the second-factor verification path before assuming it needs building.

## 3. Regressions blocking other tests

> **To confirm before coding.** Neither of these is reproducible from the repository: both descriptions
> (`MFA-74`, `MFA-75`) are what the QA runner saw on `qpylon/dev`, and the branch `epic/mfa` is not
> running there any more. Registration, confirmation and login-after-confirmation all read correctly in
> the source — confirmation goes through a single-use token and an explicit click (`ConfirmEmailMain`),
> and `UserDetailsServiceImpl` explicitly handles the no-password and inactive-account cases. So the
> first question is not "where is the bug" but "was this the instance or the code": a deployment whose
> mail slot or token parameter differs from the defaults would produce exactly these two symptoms.
>
> **Ask:** the instance URL (or its `application.properties` diff), and whether `routing.email-confirmation.token-parameter`
> was customised. With that, a reproduction is a five-minute test; without it, any "fix" here would be
> guessing at a defect nobody has seen since.

### S3 — Registration is broken on the test instance

- **Test:** MFA-74. Recorded Failure. Blocks MFA-30 and MFA-75.
- **Note:** first decide whether this is a dev-instance configuration problem or a real regression
  on `epic/mfa` — do not start coding before that is answered.

### S3 — A newly confirmed account cannot log in

- **Test:** MFA-75. Recorded Failure.
- **Symptom:** confirmation link arrives and clicks through, login with the confirmed account fails.

## 4. Functional defects

### S3 — Invalid samples reported as successfully registered

- **Test:** MFA-83. Recorded FAILURE.
- **Symptom:** invalid sample + "register" shows a success notification; nothing is stored. Valid
  samples do not appear until a manual refresh (empty table rendered first).

## 5. User-facing defects

### S4 — Recovery codes are lost irrecoverably on refresh — **fixed**

> **Fixed in `713fb4649` for the "nothing is said" half.** The codes still cannot be re-displayed —
> only hashes are stored (AUTH-R-03) — but a session marker now survives the refresh and the settings
> page states plainly that the sheet was not saved, what that costs, and that a new set is the way out.
> Verified by `RecoveryCodesUnacknowledgedSpec`.

- **Tests:** MFA-91 (FAILURE) and MFA-40 (recorded Success — contradictory, see §7).
- **Symptom:** refresh without pressing "I have saved my keys" and the codes can never be shown again.

### S4 — No warning when the last recovery code is consumed — **fixed**

> **Fixed in `775474246`.** The obvious place cannot work: redeeming a code establishes the session
> and redirects, so text written into the challenge page's components is discarded immediately. The
> count travels in the session and is rendered by `MfaEnrolmentPrompt` in the application shell,
> consumed once and not dismissible. Verified by `LastRecoveryCodeWarningSpec`.

- **Test:** MFA-41b. Recorded FAILURE.

### S4 — Enrolment confirm/cancel buttons unreachable at low resolution

- **Tests:** MFA-93 (FAILURE), plus MFA-01 observation "may not show the save button" (Success).
- **Symptom:** small window cuts off confirm and cancel, scrolling does nothing, only zoom-out helps.

### S4 — Misleading login and MFA error messages

- **Test:** MFA-94. Recorded Failure.
- **Symptom 1:** "Wrong username or password" for a correct email with a wrong password — wording
  implies username login is supported, which it is not.
- **Symptom 2:** the "That code is not valid…" message is sometimes scrolled out of view, so the user
  sees no feedback at all.

### S4 — The enrolment dialog put the optional name before the required code — **fixed**

- **Reported live:** the QR code and setup key led, then a full-width field labelled
  *"Name this app (optional)"* in accent blue, and only then *"Code from the app"*. Reading top to
  bottom, the page asked for a nickname before the one step that finishes enrolment.
- **Three inversions at once:** order (name above code), colour (a primary-coloured label reads as a
  call to action — "optional" in accent blue contradicts itself), and width (three equal full-width
  fields made the code look like one more optional input).
- **Fixed:** scan → setup key → code → name. The code gets a separator, autofocus and the full width;
  the name is narrower, quiet-grey, smaller helper text, and last. Naming still cannot gate
  enrolment, and an empty field still means "use the default".
- **Pinned by** `EnrolmentDialogHierarchySpec` — the step order is asserted from the component, so it
  cannot drift back silently.

### Not a defect: the recovery sheet dies on refresh — **accepted**

- **Reported:** refreshing while the recovery-code dialog was open closed it.
- **Decision:** correct as-is, for the reason stated in the report itself — the plaintext codes must
  not live in session state, so nothing reconstructs them after a reload. Regenerating is one click
  and retires the lost set.
- **One caveat worth keeping:** this is only benign because MFA-91 now says the sheet was not saved.
  Before that fix, a refresh discarded the codes *and* said nothing, which is the failure mode this
  behaviour would otherwise recreate.

## 6. Decide before coding

### S5 — Deep-link return after login

- **Test:** MFA-80. Recorded Failure. Currently always lands on home; the tester argues this is
  reasonable. Either implement the return or change the expected result in the plan.

## 7. Suspicious findings recorded as "Success"

Defects, contradictions or coverage gaps that did not fail their stated expectation.

- **Recovery-code defect has two verdicts** — MFA-40 (Success) and MFA-91 (FAILURE) describe the same
  bug. Treat as one item, failure verdict correct.
- **Same valid TOTP accepted twice** — MFA-20. Replay not prevented; confirm the intended policy.
- **TOTP time tolerance too wide, value unknown** — MFA-13, recorded "Done, neither pass nor fail".
- **"Contact an administrator" warning shown in OPTIONAL mode** — MFA-08.
- **No Replace action, only add; contradictory copy between the two flows** — MFA-07.
- **Stray notification when creating experiment groups** — MFA-82. Unrelated to MFA, screenshot in doc.
- **Error-page inspection by a single reviewer** — MFA-71. The tester flags that "no stack traces or
  secrets leaked" is too important for one pair of eyes; needs a second review before sign-off.

### Coverage gaps recorded as Success

- **Reauthentication before factor changes tested only within one session** — MFA-09. Stale or
  cross-session requests are untested.
- **ORCID login removal never tested on an ORCID-linked account** — MFA-16 ("not done yet").
- **Multi-node deployment forces the first factor twice** — MFA-24, possibly a session-affinity issue.

---

## Working order

1. Regression tests that capture each finding as a failing spec (this PR's first commit).
2. S1 identity confusion — highest severity, narrow fix.
3. S3 registration / confirmation regressions.
4. S2 throttling.
5. S4 user-facing defects.
6. S5 decisions.
