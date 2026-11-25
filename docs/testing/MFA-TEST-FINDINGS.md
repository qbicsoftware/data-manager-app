# MFA test findings — 2026-11-25 run

Source: `Testing MFA DataManager.docx` (environment `qpylon/dev`, branch `epic/mfa`).
97 entries, 10 recorded failures. This file is the working to-do list for `fix/mfa-feature-fixes`.

Severity key: **S1** identity compromise · **S2** missing security control · **S3** broken
functionality · **S4** user-facing defect · **S5** decide, may not need code.

---

## 1. Security: authentication identity confusion

### S1 — "Choose different account" authenticates as the previous account

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

### S2 — No throttling on repeated failed TOTP attempts

- **Test:** MFA-68. Recorded FAILURE.
- **Repro:** submit wrong codes repeatedly (~2 min of clicking) — nothing slows, blocks or locks.
- **Open question:** the implementer is unsure whether throttling exists at all; grep for a
  rate limiter on the second-factor verification path before assuming it needs building.

## 3. Regressions blocking other tests

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

### S4 — Recovery codes are lost irrecoverably on refresh

- **Tests:** MFA-91 (FAILURE) and MFA-40 (recorded Success — contradictory, see §7).
- **Symptom:** refresh without pressing "I have saved my keys" and the codes can never be shown again.

### S4 — No warning when the last recovery code is consumed

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
