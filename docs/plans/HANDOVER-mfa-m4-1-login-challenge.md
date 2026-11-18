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
| `MfaChallengeEnforcementFilter` | same | belt and braces: an authenticated session that coexists with a pending factor loses its auth on the next request |
| `SecondFactorView` | `datamanager-app/…/views/login` | `/login/second-factor`: code entry, uniform refusal message, replays the saved request on success |

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
| M4-3 recovery-code login | the codes exist and redeem at service level; the login path for them is not wired. The challenge says "ask an administrator" — true today |
| M4-4 saved request | actually works already: the view replays Spring's saved request on success |
| M4-5 bypass suite | the unit-level properties are asserted; the HTTP-level suite (second factor required on every route, session fixation, replay) is next |
| Second-factor throttling | M5-4, on the existing `auth_attempt` table |

## 5. Security notes for review

- The challenge reloads the principal through `UserDetailsService`, so the factor step grants exactly
  the authorities a password login would — nothing extra.
- The session is rotated by Spring at the password step; the challenge does **not** rotate again and the
  reason is documented in the view (rotating under a live Vaadin session is the bigger risk). If review
  disagrees, the fix is a plain servlet POST endpoint instead of a Vaadin event.
- `GraceExpired` names the cause to the user. That is safe: the password was correct, so it is not an
  account-existence oracle.
