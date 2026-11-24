# Milestone MFA — Self-governed Two-Factor Authentication

**Milestone:** `MILESTONE-MFA` — *Self-governed 2FA for all Data Manager accounts*
**Status:** 🟡 In Progress (M0 gate open — no code may start)
**Kind:** GitHub Milestone [#1](https://github.com/qbicsoftware/data-manager-app/milestone/1) (this repo had **no** milestones before this one)
**Proposed target due date:** 2027-04-30 (~24 weeks, see §3 phasing)
**Owner (proposed):** Product Owner + tech lead; security sign-off by whoever owns `SECURITY.md`
**Tracking:** every issue in this milestone carries `milestone: MFA` (GitHub field) + the `milestone:MFA` label
**Related docs (to be created under M0):** ADR `docs/adr/0011-mfa-authentication-authority.md`,
`AUTH-*` requirements in `docs/requirements.md`, feature/story entries in `docs/features.md`

### Issue ledger (M0 filed · 2026-11-10)

| # | Issue | Blocked by |
|---|---|---|
| [#1665](https://github.com/qbicsoftware/data-manager-app/issues/1665) | M0-1 tooling: labels + milestone setup | ✅ done |
| [#1666](https://github.com/qbicsoftware/data-manager-app/issues/1666) | M0-2 `AUTH-*` requirements PR (**the gate**) | — |
| [#1667](https://github.com/qbicsoftware/data-manager-app/issues/1667) | M0-3 ADR 0011 authentication authority | #1666 |
| [#1668](https://github.com/qbicsoftware/data-manager-app/issues/1668) | M0-4 register Features/Stories/Tasks | #1666 |
| [#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669) | M0-5 security & privacy note for factor data | #1666, #1667 |

M1–M7 issues are deliberately **not** filed yet: the milestone's own blocking rule (§5.6) says no
M2+ work enters a sprint while #1666 is open, and filing 20 speculative issues would make the
milestone percentage meaningless before a single decision is recorded.

---

## 0. Why this is a milestone and not a feature

The work is a security mandate with a **fixed end state** (no password-only login) but **no fixed
code location**: it touches the identity domain, the schema, two authentication filter chains, the
login UI, settings UI, the support/helpdesk process, and the deployment runbook. Grouping it as
"one feature" hides a multi-month dependency chain behind a single status field.

A milestone gives us:

- **One "are we safe yet?" switch** — the milestone closes only when enforcement is on in production.
- **Sub-scoping without losing the thread** — three Features (Enrolment, Login challenge, Rollout)
  each with their own Stories, all inside one milestone.
- **Honest tracking of the governance gate** — M0 (requirements + ADR) is an explicit, blocking
  issue, not a hidden prerequisite.

### What is explicitly *out* of scope

| Out of scope | Why | Revisit trigger |
|---|---|---|
| bwIDM2 / RegApp / FeLS federated step-up MFA | Adds a hard runtime dependency on a state-wide IdP, `iss`/`sub` stability we don't control, a 1:N OIDC identity model change, and a DPIA/SLA conversation. Rejected **for now**, not forever. | After M7 ships, evaluate federation to *retire the local password first factor* for users with a Landes-IdP account. One 0.5-day feasibility note, recorded in ADR 0011. |
| ORCID as the second factor | ORCID's `amr` claim (`pwd` vs `mfa`) is returned **only via the Member API**, not the Public API we use, and ORCID cannot be mandated for all users. | Only if Member API credentials exist *and* an enforcement story appears. |
| SMS / one-time codes by mail | Weak factor, high support and cost burden. | Not planned. |
| Actual WebAuthn enrolment of every user (§4 phase M9) | Cannot be forced before WebAuthn metrics exist; TOTP is the universal fallback in M6. | Post-milestone improvement. |
| Second factor on Personal Access Tokens | PATs are programmatic by design; requiring interactive 2FA per call breaks integrations (see §7 risk R4). | Handled as a policy decision in M4, not as a task here. |
| Hardware security key procurement | Org purchasing is outside the app lifecycle. | Optional, offered as a choice. |

---

> **Session fact (A5):** `server.servlet.session.timeout=${SESSION_TIMEOUT:30m}` — sessions are 30 minutes by
> default, not 30 days. The enforcement switch therefore does not strand long-lived sessions, and
> `AUTH-NFR-02`'s "no session established before a factor was enrolled shall satisfy AUTH-R-01" is
> satisfied by a 30-minute bound rather than by forced invalidation.

## 1a. Passkey deployment prerequisites (decision C1, 2026-11-10)

Canonical origin: **`https://rdm.qbic.uni-tuebingen.de`** (HAProxy terminates TLS, app runs on `:8080`).
WebAuthn works behind a reverse proxy — the browser only ever sees the public origin — but these
five conditions must hold, and four of them are *not* the passkey feature's problem; they are
already-latent proxy assumptions we would have to confirm anyway.

| # | Requirement | Why | Where to verify |
|---|---|---|---|
| 1 | HAProxy sends `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Forwarded-For`; app trusts them | Boot resolves `{baseUrl}` and redirect URIs from the request. Untrusted forwarded headers ⇒ app believes it is `http://127.0.0.1:8080` | `server.forward-headers-strategy` is **not set** in `application.properties`; confirm the effective strategy and that HAProxy is inside Tomcat's `internalProxies` (10/8, 172.16/12, 192.168/16, 127/8) |
| 2 | Exactly one public origin per environment | `rpId`/`allowedOrigins` must match the browser-visible origin; a staging instance on another host needs its own values | deployment config per environment |
| 3 | Session affinity (sticky sessions) on HAProxy | The WebAuthn challenge lives in the HTTP session; without affinity the assertion POST lands on another node and fails. Vaadin already needs affinity, so this is likely solved — must be confirmed, it is the #1 passkey-behind-load-balancer failure | HAProxy backend |
| 4 | `rpId` = **`rdm.qbic.uni-tuebingen.de`** (narrowest valid value), not a parent domain | WebAuthn allows the origin or any registrable suffix. The narrowest choice stops a passkey registered on a sibling subdomain from being usable here, and is not a commitment to the whole `uni-tuebingen.de` space | passkey config |
| 5 | Valid TLS certificate on the public origin | Browsers refuse `navigator.credentials` outside a secure context; no certificate ⇒ cannot even test | ops |

Two config values found while checking this, both worth verifying **independently of passkeys**:

- `spring.security.oauth2.client.registration.orcid.redirect-uri` defaults to
  `${ORCID_REDIRECT_URI:{baseUrl}/login/oauth2/code/orcid}`. `{baseUrl}` is derived from the request,
  so behind an untrusted proxy the ORCID redirect URI can be built with the wrong scheme/host. Either
  `ORCID_REDIRECT_URI` is always set in production, or condition 1 is already load-bearing for ORCID login.
- `server.servlet.session.cookie.domain` defaults to `de.uni-tuebingen.qbic`, which is **not a suffix**
  of `rdm.qbic.uni-tuebingen.de`. A cookie with a non-matching `Domain` attribute is dropped by the
  browser. Either the deployed value differs from the default, or the cookie is host-only and the
  default is dead config. Confirm before touching session handling.

## 1. Target architecture (what the milestone builds)

```
/login
  └─▶ [1] password (existing DaoAuthenticationProvider — unchanged)
        └─▶ success ⇒ session marked SECOND_FACTOR_PENDING, NO full auth
              └─▶ [2] second-factor step  (new)
                    ├── WebAuthn passkey/security key   (primary)
                    ├── TOTP app                        (universal fallback)
                    └── recovery code                   (break-glass, single use)
                          └─▶ full authentication established
```

Non-negotiable properties (these become acceptance criteria):

1. **Owned by us** — factor store and policy live in the `identity` bounded context. No external
   login dependency.
2. **Verifiable, not assumed** — the app never trusts a token claim it did not verify; it verifies
   the factor itself.
3. **Never self-locking** — an unenrolled user can always log in; a locked-out user can always
   recover (recovery code, then helpdesk-issued re-enrolment).
4. **Policy is a bean** — `MfaPolicy` (`off` / `optional` / `required`, per-user override) so the
   posture changes per cohort without a release and without an external ticket.
5. **Two factors minimum, always** — passkey **and** TOTP plus recovery codes. One factor with no
   recovery path is a guaranteed outage.

Module placement (per AGENTS.md layering):

| Concern | Module | Package |
|---|---|---|
| Second-factor domain model, policy | `identity` | `life.qbic.identity.domain.model.mfa` |
| Enrolment/challenge services | `identity` | `life.qbic.identity.application.mfa` |
| Persistent factor store, WebAuthn credential repo | `identity-infrastructure` | `life.qbic.identity.infrastructure.mfa` |
| Auth filters, session state, handlers | `datamanager-app` | `life.qbic.datamanager.security` |
| Enrolment / management / recovery UI | `datamanager-app` | `views/account/` (settings area) |
| DDL | `sql/migrations/` + `docs/migrations/NEXT.md` | — |

---

## 2. Issue map (what gets created in the milestone)

**Schema reminder:** Features `FEAT-AUTH-FLOWS`, `FEAT-MFA-ENRL`, `FEAT-MFA-LOGIN`,
`FEAT-MFA-ROLLOUT`; Stories `FEAT-<SLUG>-<ABBR>-NN`; every Task names its parent Story by **stable ID**,
not issue number. Proposed requirement IDs are in §8.

### Sequencing decision (2026-11-10): clean up first

The login, registration and account-recovery paths are consolidated **before** any second-factor
work (phase M1). Two reasons:

1. **It is a live defect, not a refactor.** Email confirmation and password reset currently act on a
   URL derived from the user's persistent identifier, on retrieval alone, with no expiry and no
   single-use. That is the path an attacker takes to *skip* a second factor entirely, so hardening
   the factor while leaving it open would be theatre.
2. **M3 rewires the same code.** Feature gates, the challenge step and the success handlers all touch
   `SecurityConfiguration`, `LoginLayout` and the identity service. Consolidating first means those
   files are rewritten once, not twice.

Phase M1 is therefore **auth-flow consolidation**, and factor storage moves to M2. Everything
numbered M2–M8 in the original plan shifts by one; this table is the authority.

### Phase M0 — Decide & document (blocking gate) · ~2 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M0-1 | Task (ops) | Create `type:*`, `sub-issue`, `milestone:MFA` labels and the `MILESTONE-MFA` milestone | `chore` | — |
| M0-2 | Task | Draft `AUTH-*` requirements (enforcement, factor ownership, secrets, recovery, audit, verification links, single identifier, uniform failures) → separate requirements PR | `documentation`, `ready2code` | M0-1 |
| M0-3 | Task | ADR 0011 — MFA authentication authority = the application (alternatives: bwIDM/RegApp, ORCID `amr`, external IdP) | `documentation` | M0-2 |
| M0-4 | Task | Register Features + Stories in `docs/features.md` and GitHub; link all issues to the milestone | `documentation` | M0-2 |
| M0-5 | Task | Security/DPIA review note: factor data, verification tokens, attempt records — what is stored, where, encryption, retention | `documentation` | M0-3 |

**Gate:** requirements PR merged **and** ADR 0011 merged with security sign-off. No code before this.
*Decision A7 (2026-11-10): the requirements stay in a single PR — [#1670](https://github.com/qbicsoftware/data-manager-app/pull/1670).*
*Deviation option (used earlier in this repo for `GROUP-*` and `FEAT-DATSET-*`): proceed under
recorded PO direction with the justification in the PR description. Not recommended here — this is
a security mandate and the requirement text is the artefact auditors will ask for.*

### Phase M1 — Auth-flow consolidation *(the clean-up, ahead of the factor)* · ~4 weeks

Feature: **`FEAT-AUTH-FLOWS`** — login, registration and account recovery behave as one coherent
journey. Requirements: `AUTH-R-05`, `AUTH-R-06`, `AUTH-NFR-03` (+ `AUTH-R-01` for the baseline).

| # | Rel. | Type | Title | Labels | Depends on |
|---|---|---|---|---|---|
| M1-1 | R1 | Task | **Tokenised verification links**: `user_verification_token`, hashed single-use tokens, expiry, supersede-on-reissue, explicit confirm action (`sql/migrations/auth-flow-hardening.sql` — promoted) | `complex`, `high prio` | M0 gate |
| M1-2 | R1 | Task | **Confirmation moves off `/login`**: dedicated `/registration/confirm` route; `LoginLayout.handle()` no longer mutates state on GET | `medium` | M1-1 |
| M1-3 | R1 | Task | **Reset flow uses tokens**: `/registration/reset-password` stops accepting a user id; `SetNewPasswordMain` consumes a token | `complex`, `high prio` | M1-1 |
| M1-4 | R1 | Task | **Uniform failure responses** (`AUTH-NFR-03`): reset form always reports "if an account exists, a link was sent"; constant-work login path so unknown account and wrong password are indistinguishable | `medium` | M1-3 |
| M1-5 | R1 | Task | **Server-side throttling**: `auth_attempt` for login, resend-confirmation, resend-reset; replaces the client-side 30 s `Timer` in `EmailConfirmationMain` | `medium` | M1-4 |
| M1-6 | R2 | Task | **One registration flow**: merge `UserRegistrationComponent` / `UserRegistrationOrcIdComponent` (identical validator chains) and the duplicated `handleRegistrationFailure` switches | `medium` | — |
| M1-7 | R2 | Task | **One password policy**: single source (`PasswordPolicy`), fix the "shorter than 8 characters" message for a 12-character rule, remove the three UI copies | `simple` | — |
| M1-8 | R2 | Task | **Display name is not a credential** (`AUTH-R-06`): stop asking for a user name at registration (derive or defer to profile); keep `platformUserName` as display-only for sharing and avatars | `medium` | M1-6 |
| M1-9 | R2 | Task | Remove dead UX state (`?userRegistered` consumer with no producer) and collapse the duplicated confirmation-parameter configuration | `simple` | M1-2 |
| M1-11 | R1 | Task | **ORCID-only accounts get a password** (`AUTH-R-07`, decision D1b): detection query, one-time set-password invitation for accounts with `encryptedPassword IS NULL`, remove the passwordless-ORCID registration path, make ORCID login require an existing link instead of offering registration (runbook in `docs/migrations/NEXT.md`; the DDL itself is promoted) | `complex`, `high prio` | M1-1, M1-3 |
| M1-10 | R1 | Task | Migration guide entry in `docs/migrations/NEXT.md` for both new tables, with the "links in flight stop working" operator note (B1) | `documentation` | M1-1 |

**Two releases (decision B5):** `R1` is the security fix and ships first; `R2` is the consolidation
and follows. Rationale: the vulnerability fix must not wait on a UI refactor, and if the cosmetic
change causes a regression it must be revertable without reverting a security fix.

- **R1 (weeks 3–5):** tokenised links, confirmation off `/login`, tokenised reset, uniform failures,
  server-side throttling, migration guide. Gate: *a link works once, expires, and states the change
  only after an explicit click; the reset form reveals nothing about account existence; no flow
  accepts a user id as a secret.*
- **R2 (weeks 6–7):** one registration flow, one password policy, display name no longer a credential,
  dead state removed. Gate: *one registration form, one password rule rendered from one place,
  no credential-shaped field that is not a credential.*

**Decisions applied (2026-11-10):**
- **B1 — no compatibility window.** The old `?confirm-email={userId}` route is removed, not kept
  alongside. Links sitting in inboxes stop working; the release note and the helpdesk script say
  "request a new link". No dual-mode code path is ever shipped.
- **B2 — `platformUserName` is display-only.** Registration stops asking for it (derived from the
  email local part when needed, editable in the profile); existing values are preserved because
  project sharing, avatars and the person picker read them.
- **B3 — lifetimes:** confirmation ≤ 1 h, reset ≤ 15 min, enforced as bounds in `AUTH-R-05`
  (initial configuration: 1 h / 5 min), with an always-available "send a new link".
- **B4 — throttling thresholds are not fixed here.** They are deployment configuration, tuned from
  telemetry. Two rules that *are* fixed: (a) count per account **and** per origin pair, never per
  account alone, so an attacker cannot lock a known victim out by spamming their address; (b) a
  throttled response must be indistinguishable from a normal one (`AUTH-NFR-03`), otherwise the
  throttle itself becomes the enumeration oracle.
- **B5 — two releases**, as above.
- **D1b — every account holds a password** (`AUTH-R-07`). ORCID becomes a link only. Migration for
  existing passwordless accounts: they are **not** deleted and **not** force-emailed in bulk. Detection
  query + a one-time "set your password" invitation. Two facts make this safe: those accounts are
  `active = false` today (they never confirmed their email, so they cannot log in at all) and are
  unlinked from nothing — so the invitation is a normal confirmation-style link carrying a password
  reset token, and the first click both verifies the address and sets the credential.
- **Two CLA conflicts created by D1b**, both needing resolution before M1/R1 ships:
  1. **PRD §Technical Constraints** states *"ORCID OAuth2 integration for researcher identity; local
     password-based authentication as fallback"* — D1b inverts the priority. Needs a PRD edit (PO-owned,
     not an agent edit).
  2. **`AUTH-C-01`** forbids external providers as the *second*-factor enforcement point; under D1b the
     provider is not a factor at all. The constraint still holds but its wording now understates the
     position, and should be revisited when the PRD changes.

### Phase M2 — Factor storage · ~2 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M2-1 | Task | Schema migration: `user_second_factor`, `user_recovery_code`, `user_login_throttle` + `users` MFA columns (draft: `sql/migrations/drafts/mfa-second-factor.sql`) | `chore`, `complex` | M0 gate, M1 gate |
| M2-2 | Task | `SecondFactor` domain model + `SecondFactorRepository` port in `identity` | `complex` | M2-1 |
| M2-3 | Task | JPA implementation in `identity-infrastructure`; secrets encrypted with the existing vault key (same pattern as personal access tokens) | `complex` | M2-2 |
| M2-4 | Task | `MfaPolicy` bean (`off`/`optional`/`required` + per-user override) and its configuration properties | `medium` | M2-2 |
| M2-5 | Task | Migration operator guide entry in `docs/migrations/NEXT.md` (pre-flight / apply / verify / rollback) | `documentation` | M2-1 |

> Note: the reuse of `user_login_throttle` in M2-1 overlaps `auth_attempt` from M1-5. Decide during
> M1-5 whether one table serves both (recommended) and drop the other from the drafts, rather than
> shipping two throttling tables.

### Phase M3 — Enrolment (spike) · ~3 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M3-1 | Task **spike** | TOTP enrolment end-to-end: secret generation, QR, RFC 6238 validation (±1 period skew), activation, audit event | `complex` | M2-3 |
| M3-2 | Task **spike** | WebAuthn `WebAuthnCredentialRepository` over our table + `rpId`/`allowedOrigins` config. **Optional extension** (`AUTH-R-02`): the milestone is complete without it; the spike decides whether it ships. Deployment prerequisites in §1a below | `complex` | M2-3 |
| M3-3 | Task | Settings surface: "Two-factor authentication" section (TOTP baseline + recovery codes; passkey entry point only if M3-2 ships) | `medium`, `UI/UX` | M3-1 |
| M3-4 | Task | Recovery-code generation, download/print, one-time consumption. With no exempt accounts these are the *only* self-service way past a lost device, so the flow is load-bearing, not a nicety | `medium` | M3-1 |

**Spike exit criteria:** an enrolled user can complete enrolment on desktop + mobile; the TOTP secret
is unreadable in the DB; a WebAuthn credential survives an application restart; the recovery codes
work exactly once.

### Phase M4 — Login challenge · ~3 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M4-1 | Task | Second-factor step in the login flow: `SECOND_FACTOR_PENDING` session state, challenge page, no full auth until verified | `complex` | M3-1 |
| M4-2 | Task | WebAuthn assertion path (discoverable credentials) wired into the challenge | `complex` | M3-2, M4-1 |
| M4-3 | Task | Recovery-code login path + "lost my factor" entry point | `medium` | M3-4, M4-1 |
| M4-4 | Task | Redirect handling: intercepted deep links survive the challenge (saved-request cache) | `medium` | M4-1 |
| M4-5 | Task | **Bypass test suite**: password alone must not yield an authenticated session (Vaadin routes, API paths, session fixation, concurrent sessions); re-verify the M1 token flows cannot be used to skip the factor | `complex` | M4-1 |

**Spike exit criteria (the risk that matters):** with an enrolled account, *every* path that grants a
session requires the second factor. Attempts to skip it (`/login` replay, direct route hit, session
reuse, a stale verification link) fail closed.

### Phase M5 — Enforcement & break-glass · ~2 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M5-1 | Task | Enforcement gate: `required` mode with per-account 48 h grace window; whitelist for enrolment/logout endpoints | `complex` | M4-1 |
| M5-2 | Task | Mandatory enrolment in the **registration** flow (new accounts) — cheapest phase, no legacy population | `medium` | M4-1 |
| M5-3 | Task | **No-exemption check:** verify no account, flag, role, or code path bypasses the factor; add a test that asserts an empty exemption set, and document the "second factor store is unavailable" posture (deny, do not bypass) | `medium` | M5-1 |
| M5-4 | Task | Throttling for repeated second-factor failures, built on the `auth_attempt` mechanism delivered in M1-5 (do **not** add a second throttling table) | `medium` | M2-1, M1-5 |
| M5-5 | Task | Audit log entries: enrolment, factor use, recovery use, admin re-enrolment | `medium` | M2-3 |

### Phase M6 — Recovery & support operations · ~2 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M6-1 | Task | Admin-triggered re-enrolment (no DB access needed) — the last resort now that no account is exempt | `medium` | M5-1 |
| M6-2 | Task | Helpdesk runbook: scripts for "lost phone", "lost recovery codes", "new device", escalation | `documentation` | M6-1 |
| M6-3 | Task | User-facing FAQ + enrolment instructions (in-app + external) | `documentation`, `UI/UX` | M3-3 |
| M6-4 | Task | Ops reporting query: enrolment %, unenrolled accounts, exception list | `documentation` | M2-1 |

### Phase M7 — Pilot (internal + volunteers) · ~4 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M7-1 | Story | Required for all QBiC staff/admins, optional for pilot labs | `type:story` | M5 gate |
| M7-2 | Task | In-app enrolment banner with honest deadline copy (only in `optional`/`required` modes) | `UI/UX` | M3-3 |
| M7-3 | Task | Metrics capture: enrolment rate, first-login drop-off, tickets per 100 users | `medium` | M5-5 |

**Pilot gate:** ≥90 % of internal staff enrolled, zero unexplained lockouts, ticket categories known.

### Phase M8 — Enforcement rollout · ~6 weeks

| # | Type | Title | Labels | Depends on |
|---|---|---|---|---|
| M8-1 | Story | Deadline announced once, countdown repeated at 4/2/1 weeks; then `required` for all local accounts | `type:story` | M7 gate |
| M8-2 | Task | Flip `MfaPolicy` to `required` per cohort; daily exception report to ops | `medium` | M8-1 |
| M8-3 | Task | Post-rollout review: metrics, incidents, support load; close milestone | `documentation` | M8-2 |

### Phase M9 — Steady state (post-milestone) · ongoing

| # | Type | Title | Labels |
|---|---|---|---|
| M9-1 | Task | Annual policy review; PAT/service-account posture documented | `documentation` |
| M9-2 | Task | Optional: passwordless (passkey-first) login once passkey coverage is measured | `enhancement` |
| M9-3 | Task | Optional: federated (bwIDM/RegApp) feasibility note to retire the local password first factor | `enhancement` |

---

## 3. Phasing on a timeline (proposed)

| Weeks | Phase | Milestone % complete (issues closed / total ≈ 45) |
|---|---|---|
| 1–2 | M0 Decide & document | 10 % |
| 3–5 | **M1 / R1 — verification-token security fix** | 26 % |
| 6–7 | **M1 / R2 — registration consolidation** | 34 % |
| 7–8 | M2 Factor storage | 43 % |
| 9–11 | M3 Enrolment spike | 53 % |
| 12–14 | M4 Login challenge spike | 64 % |
| 15–16 | M5 Enforcement & break-glass | 75 % |
| 17–18 | M6 Recovery & support | 82 % |
| 19–22 | M7 Pilot | 89 % |
| 23–28 | M8 Rollout to `required` | 100 % |

Sequencing rules that must not be inverted:

1. **M1 before M2** — the clean-up closes the paths that would otherwise bypass the factor.
2. **M5-2 (mandatory at registration) before M8** — it stops the unenrolled legacy population from
   growing while the rest is being built.

---

## 4. Milestone exit criteria (definition of "done" for the milestone)

**Auth-flow consolidation (M1) — these must hold before any factor work is called done:**

- [ ] No flow accepts a user identifier as a secret: confirmation and reset links carry single-use,
      time-limited tokens, and a token is dead after one use, after re-issue, and after expiry.
- [ ] No unauthenticated GET mutates account state.
- [ ] Login, confirmation, and reset responses are uniform in wording and timing; the reset form no
      longer reports "user not found" / "user not active".
- [ ] Throttling on these flows is server-side (survives a page reload), not a client-side timer.
- [ ] One registration flow, one password policy, one place that renders registration errors.
- [ ] A user name is never accepted as a credential.

**Second factor:**

- [ ] `MfaPolicy` = `required` in production for all local accounts; login without a second factor is
      impossible except for the documented break-glass account.
- [ ] New accounts cannot be created without completing enrolment.
- [ ] Passkey **and** TOTP **and** recovery codes all work end-to-end; a user can recover from a lost
      device without developer or DB intervention.
- [ ] Bypass test suite green; lockout/throttling active on both password and second-factor steps.
- [ ] Audit trail covers enrolment, use, recovery, admin re-enrolment.
- [ ] ADR 0011 + `AUTH-*` requirements merged; `docs/features.md` stories closed.
- [ ] Operator migration guide released (`docs/migrations/`), runbook and FAQ published.
- [ ] Post-rollout review recorded with support-load numbers.

---

## 5. Tracking conventions (so the milestone stays usable)

1. **Every issue in the milestone** has `milestone: MFA` (GitHub field) and the `milestone:MFA` label
   (so it survives project-board filtering and shows up in `gh issue list --label milestone:MFA`).
2. **No orphan issues:** Task → Story → Feature → Requirement, per AGENTS.md §0. Tasks reference the
   stable Story ID in the body, never the issue number.
3. **One acceptance-criteria owner:** the Story owns the AC; Tasks copy it and never redefine it.
4. **Spike tasks carry a timebox** (label `spike` + a comment with the deadline) and end in a written
   decision, even if the decision is "no".
5. **Weekly milestone check** on the same day as the sprint review: count open/closed, flag any issue
   without a dependency-satisfied predecessor.
6. **Blocking rule:** no M2+ issue moves to `sprint:current` while the M0 gate issue is open.

### Ready-to-run setup (not executed yet)

```bash
# Labels the issue templates already reference but the repo does not have:
gh label create "type:feature" --color "c2e0c6" --description "Feature issue (FEAT-<SLUG>)"
gh label create "type:story"   --color "1ab6a2" --description "Story issue (FEAT-<SLUG>-<NN>)"
gh label create "type:task"    --color "bfe693" --description "Technical task under a Story"
gh label create "sub-issue"    --color "ededed" --description "Sub-issue of a larger issue"
gh label create "milestone:MFA" --color "B60205" --description "Self-governed 2FA milestone"
gh label create "spike"        --color "F6E120" --description "Timeboxed investigation, decision required at end"

# The milestone itself:
gh api repos/:owner/:repo/milestones \
  -f title="MILESTONE-MFA: self-governed two-factor authentication" \
  -f state=open \
  -f due_on=2027-04-30T00:00:00Z \
  -f description="Make a second factor mandatory for all Data Manager logins, with our own factor store. Exit: no password-only login in production."
```

---

## 6. Dependencies and blockers

| Blocker | Blocks | Mitigation |
|---|---|---|
| M0 requirements/ADR approval | everything | Start the PR in week 1; keep M1 DDL reviewable in parallel as a draft |
| Schema change approval (`user_verification_token`, `auth_attempt`, `users` columns) — AGENTS §12 | M1, M2 | Bundle into the M0 PR discussion so approval is given once, not twice |
| `datamanager-bom` additions (`spring-security-webauthn`, one TOTP lib) | M2 | BOM-only PR; `sigcheck` profile requires PGP signatures — verify before choosing the TOTP lib |
| Browser/device support | M2–M3 | TOTP covers everything; WebAuthn is offered, not required |
| Helpdesk capacity in M6/M7 | M7 | Publish runbook in M5, not during rollout |
| Project-board sprint automation (`label_sprint_issues.yml`) | tracking | It only mirrors `sprint:current`; the milestone is independent, so no workflow change is needed |

---

## 7. Risks

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R1 | Users locked out during enforcement | High (support storm, trust loss) | Grace window, break-glass, recovery codes, helpdesk script ready **before** M7 |
| R2 | Users lose their only factor | High | Two factor types + single-use recovery codes + admin re-enrolment |
| R3 | Secrets mishandled in the DB | High | Vault-encrypted secrets; WebAuthn stores public keys only; DPIA note in M0-5 |
| R4 | PATs and integrations break | Medium | PAT posture decided in M4, documented; no interactive challenge for token auth |
| R5 | Scope creep into federation (bwIDM) | Medium (schedule) | Explicitly out of scope with a revisit trigger (§0) |
| R6 | Enforcement deployed without the registration gate | Medium | M4-2 sequenced before M7; milestone checklist enforces it |
| R7 | No lockout today means brute force is untested territory | Medium | M5-4 covers second-factor throttling on the `auth_attempt` mechanism from M1-5 |
| R8 | **Vault key loss is now a total lockout.** Accounts are **not** exempt (A1), so if the key protecting TOTP seeds is unrecoverable, no local user can log in until re-enrolled — and re-enrolment requires an authenticated session | **High** | Recovery codes are stored *outside* the vault-encrypted seed path (hashed in `user_recovery_code`), so they survive a vault-key loss; verify this explicitly in M3-4 and in the security note (#1669). Restore of the key must be part of the DR runbook |
| R9 | Recovery codes become the single weakest link | High | High-entropy codes, single use, regeneration invalidates all; treat a compromised code as a full account compromise in the incident process |

---

## 8. Requirement IDs (drafted in the M0-2 PR, [#1670](https://github.com/qbicsoftware/data-manager-app/pull/1670))

| ID | Statement (one line) |
|---|---|
| `AUTH-R-01` | The system shall require a second authentication factor for every interactive human login. |
| `AUTH-R-02` | The system shall let users enrol and manage multiple second-factor credentials (passkey/security key, TOTP) themselves. |
| `AUTH-R-03` | The system shall provide single-use recovery codes and an administrative re-enrolment path so users cannot be permanently locked out. |
| `AUTH-R-04` | The system shall record enrolment, factor use, recovery use, and administrative re-enrolment in the audit log. |
| `AUTH-NFR-01` | Second-factor secrets shall be stored encrypted; WebAuthn credentials shall be stored as public keys only. |
| `AUTH-NFR-02` | Second-factor verification shall complete within 500 ms at p95 and shall fail closed. |
| `AUTH-R-05` | Confirmation and reset links shall use unguessable, single-use, time-limited tokens; consuming a link shall require an explicit user action. |
| `AUTH-R-06` | A user shall be authenticated by exactly one identifier — the verified email address; any name is display-only. |
| `AUTH-NFR-03` | Authentication failures shall be indistinguishable in content and timing, and throttled per account and per origin. |
| `AUTH-C-01` | The second factor shall not depend on an external identity provider for enforcement. *(constraint — referenced only in Task notes)* |

---

## 9. First three actions (week 1)

1. ~~Create labels + milestone~~ — done: labels exist, milestone [#1](https://github.com/qbicsoftware/data-manager-app/milestone/1)
   created, M0 issues [#1665](https://github.com/qbicsoftware/data-manager-app/issues/1665)–[#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669) filed.
2. ~~Open the requirements PR~~ — done: [#1670](https://github.com/qbicsoftware/data-manager-app/pull/1670) is open with all ten
   `AUTH-*` entries. **Awaiting human approval — it is the gate.**
3. Hand both drafts to review in parallel, so schema approval and requirement approval land in the
   same conversation:
   - `sql/migrations/auth-flow-hardening.sql` (M1, promoted)
   - `sql/migrations/drafts/mfa-second-factor.sql` (M2)
