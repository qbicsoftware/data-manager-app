# Handover — auth-flows R2 and the security patch

**Written:** 2026-11-13 · **Working branch for the next slice:**
`hotfix/security-auth-hardening` (PR [#1713](https://github.com/qbicsoftware/data-manager-app/pull/1713), base `main`)

Read this first, then
[`HANDOVER-auth-flow-hardening.md`](HANDOVER-auth-flow-hardening.md) for the R1 slice, and
[`MILESTONE-MFA-self-governed-2fa.md`](MILESTONE-MFA-self-governed-2fa.md) for the milestone.

---

## 1. State of play

| Work | Where | State |
|---|---|---|
| Tokenised single-use confirmation / reset links | `development` (#1689) | ✅ merged |
| Server-side throttling + uniform failures | `development` (#1690) | ✅ merged |
| Registration stops disclosing account existence | `development` (#1693) | ✅ merged |
| §12 sign-off for `auth_attempt` | `development` (#1711) | ✅ merged · recorded on #1690 |
| **All four, as a patch off `main`** | **#1713** | 🟡 **open, awaiting release checks** |

`FEAT-AUTH-FLOWS-03` (#1675) stays open on exactly **one** criterion: per-origin throttling, which needs the
proxy work in #1691 and cannot be written from this repository. Everything else in that story is done.

---

## 2. The next slice — R2 remainder (`FEAT-AUTH-FLOWS-04`, #1676)

**Base the branch on `hotfix/security-auth-hardening` and open the PR against it**, not against
`development`. Reason: the hotfix is the code that will be in production, and the next slice touches the
same registration views. Building on it means one review surface, and `development` gets both via the hotfix
merge.

### In scope

**2.1 Merge the two registration forms.** `UserRegistrationComponent` (231 lines) and
`UserRegistrationOrcIdComponent` (214 lines) are ~80% identical: same binder chain, same field layout, same
notification rendering, parallel `…Event`/`…Information` inner classes. The real difference is that the ORCID
variant has no password field and prefills from OIDC claims.

`#1693` already removed the two duplicated *decision* paths, so what is left is the duplication in the view
layer only. Acceptance: *both are the same component with the same validation rules and the same error
messages.*

**2.2 Remove the dead `?userRegistered` state.** `LoginLayout` renders a "Registration mail sent" notice from
a query parameter that **nothing produces** — the only other `userRegistered` occurrences are an unrelated
integration-event topic in `identity`/`project-management`. R2 acceptance: *no dead state is rendered.*

**2.3 Add a `REGISTRATION`-aware test for the merged form.** Whatever shape 2.1 takes, `#1693`'s guards must
keep applying: `UserRegistrationAccountOracleSpec` (no account-existence call from the field) and
`RegistrationThrottleWiringSpec` (the throttle is consulted before registration work) both assert on the
concrete component/route classes.

### Explicitly NOT in scope — PO decision, 2026-11-13

**`AUTH-R-06` display name: "stop asking for a user name at registration".** Deferred; not mission critical
for the security enhancement. Recorded here so it is not re-derived:

- `platformUserName` is read in 10+ display-facing places — project sharing
  (`ProjectSharingComposer`, `ProjectSharingDrawer`, `ProjectAccessComponent`), the account header and
  profile, and the group administration views. Collaborators are chosen **by name**.
- Uniqueness is enforced **only in application code** (`IdentityService.findByUserName`); there is **no**
  database constraint.
- Deriving a name from the email local part needs a collision rule: `sven+school@…` and `sven@…` both
  derive to `sven`.

Note that `AUTH-R-06`'s *security* rule already holds: the name is not a credential — login accepts only the
email address, and authorisation uses `PrincipalSid(userId)`, not the name. What remains is a UX change.

---

## 3. What is still open, in order

| # | Item | Why it waits |
|---|---|---|
| 1 | **Release #1713** | Two migrations must be applied to a copy of the production DB (validated on throwaway MariaDB only), the build must be booted once, and the helpdesk must be briefed (#1712) |
| 2 | **R2 remainder (§2)** | The next slice |
| 3 | **ADR 0011 (#1667)** | The M0 gate. The milestone says no M2 work starts without it, and #1669 (factor-data privacy note) is blocked behind it. AGENTS.md §12 requires human approval before an agent creates an ADR |
| 4 | **`AUTH-R-07` / story `-05` (#1677, #1683)** | **Not startable.** Three questions in `docs/migrations/NEXT.md` §"Execution" are unanswered: who triggers the set-password invitation, how double-issue is prevented, and what happens when the mailbox is dead |
| 5 | **M2 factor storage** | No issues filed. Blocked by #1667 |
| 6 | **Origin-scope throttling (#1691)** | Needs HAProxy changes; not in this repository |

---

## 4. Facts established this session — do not re-derive

| Question | Answer |
|---|---|
| Does the app normalise email subaddressing (`+tag`)? | **No.** The regex allows `+`, nothing strips it; `a+1@x` and `a@x` are distinct accounts. Writes are harmless but create extra accounts |
| Are `Sven@x` and `sven@x` two accounts? | **No.** Comparison is case-insensitive at the DB level (`utf8mb4_unicode_ci`) |
| Is `userName` an ACL identity? | **No.** Authorisation uses `PrincipalSid(userId)`; the name is display-only |
| Is there a DB unique constraint on `userName`? | **No.** Application code only |
| Does an inactive (unconfirmed) account get a password-reset mail? | **No.** `IdentityService.requestPasswordReset` returns success and dispatches nothing. This is why "use Forgot your password" is only correct advice for an *active* account |
| Are sessions replicated across app instances? | **No.** No `spring-session`, no Hazelcast. Vaadin already requires sticky sessions, so the whole UI — not just the new session attribute — depends on affinity |
| Which sessions matters for the pending-confirmation page? | The page reads the pending account id from `VaadinSession` (key `EmailConfirmationMain.PENDING_REGISTRATION_USER_ID`). On session loss it explains the situation and points at support; there is no self-service path, by design |
| Version state | `main` = **1.20.2**, `development` = **1.19.2**. After the patch releases, bump `development` to the released version or a later development→main merge moves the version backwards |

---

## 5. Verification status — read before claiming anything works

| Check | State |
|---|---|
| Auth specs on a `development` base | ✅ 175 passed |
| Auth specs on the **`main`** base (hotfix branch) | ✅ 175 passed, compiles |
| Runtime: pending-confirmation URL carries no user id | ✅ confirmed by the PO on a real build |
| Runtime: pending page renders all three states | ✅ confirmed for the session-expired state |
| Runtime: new registration mails / duplicate mails nothing / resend sends exactly one | ⚠️ **not yet confirmed** |
| Migrations applied to a copy of real data | ⛔ **never done** |
| SonarCloud coverage gate on #1713 | ⚠️ **unknown** — the gate failed at 15 % and 29 % on earlier revisions of #1693; the final figure was not read back |

---

## 6. Gotchas in this codebase that cost time

- **`IdentityService` is `final`** → cannot be mocked. Registration now goes through the narrow port
  `RegistrationIdentity`; do the same rather than reaching for a mock.
- **No Objenesis on the test classpath** → `Stub(VaadinService)`-style mocks of classes with no no-arg
  constructor fail. Use real instances with literal arguments (`EmailConfirmationLinkSupplier` is a working
  example in three specs).
- **`UserInfo` is a record** → construct it directly; it cannot be mocked.
- **`Text` is a client-side text node** → `addClassName` on it throws. Wrap in a `Span`.
- **`Stub(VaadinSession)` breaks `RouterLink` construction**, which resolves through the session's route
  registry. Install a session stub only around `beforeEnter`, not for the whole spec.
- **`0 * mock.x()` needs a `Mock`**, not a `Stub`.
- **Fully-qualified names inside Groovy closures** parse as property access —
  `Stub(life.qbic.SomeClass)` fails with `MissingProperty: life`. Import instead.
- **`git checkout -- <file>` discards uncommitted work.** It cost a rewrite of
  `EmailConfirmationMain` this session. Copy to `/tmp` before reinstating a bug for a guard proof.
- **Guard proofs must reinstate the *whole* bug.** A half-reinstated two-part bug passes — that happened in
  the R1 slice.

---

## 7. Deployment contract for #1713

**Apply both migrations before deploying the code.** Neither table exists on 1.20.2, and each is queried on
the first request that needs it:

| Missing | Consequence |
|---|---|
| `user_verification_token` | No link can be issued or consumed — and the failure is **silent**: registration appears to succeed, no link works |
| `auth_attempt` | **Login fails outright.** `LoginThrottleFilter` queries it on the first attempt |

**Links already in flight stop working** (decision B1). Confirmation and reset URLs already in inboxes are
dead after the release, by design — the old link *was* the credential. Helpdesk script: *request a new link
from the login page; do not ask the user to resend the original email.* Tracked in #1712.

**Rollback is not `DROP TABLE`.** Both tables are additive and inert without the code, so a revert means
redeploying 1.20.2 and **leaving the schema in place**. Dropping `user_verification_token` invalidates every
unconsumed link and destroys the audit trail of issued/consumed/superseded tokens, and it does not restore
the pre-change links either. This is written into both rollback sections of `docs/migrations/NEXT.md`.

---

## 8. The lesson that keeps holding

Every defect that mattered in this slice was found by reading the running product, not by the suite:

- an account-existence oracle in **field validation**, while the submit path had already been made uniform;
- a duplicate registration that mailed the account owner about a password reset they never requested, and
  invalidated any reset link they were waiting for;
- the user id **in the URL** of the pending-confirmation page, after it had been deliberately removed as a
  credential elsewhere;
- one blended page message that advised the reset flow to visitors whose account was unconfirmed, where a
  reset request dispatches nothing.

So: keep writing the specs, and **run the application before saying it works**. When a page or flow changes,
walk it in a browser with a fresh session, an existing account and an expired session.
