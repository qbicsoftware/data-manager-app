# Handover — MILESTONE-MFA phase M4-1 (login challenge)

**Written:** 2026-11-18 · **Branch:** `feat/mfa-adr-0011` (base `epic/mfa`)
**Prerequisites:** ADR [0011](../adr/0011-mfa-authentication-authority.md), M2 store, M3 enrolment.

---

## 1. What is wired now

| Piece | Where | Job |
|---|---|---|
| `MfaPolicy` | `identity/…/application/mfa` | `OFF` (default) / `OPTIONAL` / `REQUIRED`, grace duration; a typo fails at startup |
| `MfaLoginGuard` | same | reads enrolment + stored grace, runs `MfaGate`, persists a granted window |
| `SecondFactorVerificationService` | same | the only place a code is accepted; `VERIFIED` / `REJECTED` / `NO_FACTOR` / `UNAVAILABLE` |
| `MfaLoginSuccessHandler` | `datamanager-app/…/security/mfa` | third success handler: on `Challenge`, stores a pending login and **removes the authentication from thread and session** |
| `MfaPendingLogin` | same | session record (user id, email, timestamp), 5-minute lifetime, not an `Authentication` |
| `MfaChallengeEnforcementFilter` | same | **authoritative**: decides per authenticated request (once per session) and withdraws the session when a factor is due |
| `MfaSessionState` | same | session marker "this login passed the step", bound to the user id, so the filter does not query the database on every request |
| `SecondFactorView` | `datamanager-app/…/views/login` | `/login/second-factor`: TOTP code entry **or a recovery code** (M4-3), uniform refusal message, replays the saved request on success |

## 1a. Why the filter, and not only the success handler (learned on the first deployment)

The success handler is the intended place, but it is only reached if the login filter chain keeps it —
and on the first real deployment it did not: mode `OPTIONAL`, account enrolled, no challenge, and no
`Second-factor gate` log line at all, i.e. the handler never ran (a framework configurer replaced it).

So enforcement moved to the filter, which runs on every request and cannot be configured away:
no marker for this session → ask `MfaLoginGuard` once → `Challenge` withholds the session and starts
the challenge, `GraceExpired` denies, everything else marks the session satisfied. The success handler
remains as the primary path (it saves one redirect) but nothing depends on it any more.

Consequence worth knowing: the marker is per session, so a policy/grace change only affects sessions
created after it (30-minute session timeout bounds the difference).

## 2. Why the authentication is removed twice

Spring saves the security context into the session **before** it calls the success handler. Clearing
only `SecurityContextHolder` would leave a fully authenticated session in the session store, restored
on the next request — the exact bypass AUTH-R-01 forbids. The handler therefore clears both, and the
enforcement filter re-checks the property on every later request. The bypass suite (M4-5) must assert
"no authenticated session after the password POST", not "the redirect points at the challenge".

## 3. How to try it

1. `qbic.security.mfa.mode=OPTIONAL` (you are enrolled → challenged) or `REQUIRED` (unenrolled → grace
   window granted and persisted).
2. Sign in: the password step lands on `/login/second-factor`, not on the app.
3. Wrong code → refused, stays on the page (same message for every refusal reason).
4. Right code → session established, saved deep link replayed.
5. With `REQUIRED` and no factor: first login grants 48 h, shown as `GraceGranted`; after it expires the
   user is denied with `?graceExpired`.

**If the challenge does not appear** at step 2, Vaadin's configurer overrode the success handler. The
enforcement filter then still strips the session on the *next* request and redirects, so the property
holds — but say so, because the flow is one redirect uglier and the handler needs to move into a filter.

## 4. Deliberately not in this slice

| Task | Note |
|---|---|
| M4-2 WebAuthn assertions | `SecondFactorVerificationService` skips `WEBAUTHN` factors: a code cannot satisfy them, and nothing creates them yet (M3-2 not shipped) |
| M4-3 recovery-code login | **done 2026-11-18**: the challenge has a "Use a recovery code instead" toggle; `RecoveryCodeService.redeemFor` consumes the code only if it belongs to the account that is mid-login, so a stranger's code is refused *without being spent* |
| M4-4 saved request | actually works already: the view replays Spring's saved request on success |
| M4-5 bypass suite | the unit-level properties are asserted; the HTTP-level suite (second factor required on every route, session fixation, replay) is next |
| Second-factor throttling | M5-4, on the existing `auth_attempt` table |

## 4a. The helpdesk is a second-factor bypass until it has an identity check

The first version of the challenge told a user with neither factor to "ask your data steward — an
administrator can reset the second factor". That is not a documentation gap, it is a hole: accounts are
created by self-service registration with nothing but email verification, so "prove you are this person"
currently means "control this inbox". An advertised email-reset path therefore hands an attacker the
cheapest way past the factor — phish the mailbox, ask for a reset — and it targets exactly the accounts
whose factor was set up to survive that.

The page now says that a reset cannot be granted by email and that identity verification is required,
and names support only to find out who verifies for a group.

**M6-1 must therefore be more than an admin button.** Before `REQUIRED` is enabled:

1. Identity proofing that does not rest on the mailbox (data steward in person, or an affiliation
   check against the institution).
2. Four-eyes or a second approver for a factor reset, since one admin account becomes the
   highest-value target in the system.
3. An audit record of who reset whose factor, when, and on what evidence (`AUTH-R-04`).
4. A helpdesk script that *refuses* an email-only request, with the exact wording (M6-2), so the
   process is uniform rather than dependent on who answers the ticket.

Until then, recovery codes are the only reset-free way in, which is why their generation must not be
skippable — see the next item.

Same class of debt for the one-time recovery-code sheet: it lists the codes but offers no
copy/download/print, so a user must transcribe ten codes by hand — the most likely way to lose them.
That is a UX defect on the break-glass path, not a nice-to-have.

## 5. Security notes for review

- The challenge reloads the principal through `UserDetailsService`, so the factor step grants exactly
  the authorities a password login would — nothing extra.
- The session is rotated by Spring at the password step; the challenge does **not** rotate again and the
  reason is documented in the view (rotating under a live Vaadin session is the bigger risk). If review
  disagrees, the fix is a plain servlet POST endpoint instead of a Vaadin event.
- `GraceExpired` names the cause to the user. That is safe: the password was correct, so it is not an
  account-existence oracle.
