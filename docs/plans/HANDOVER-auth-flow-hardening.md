# Handover — FEAT-AUTH-FLOWS R1 (auth-flow hardening)

**Written:** 2026-11-11 · **Branch:** `feature/auth-throttling-and-uniform-failures` ·
**PR:** [#1690](https://github.com/qbicsoftware/data-manager-app/pull/1690) (open, awaiting §12 sign-off
on the throttling table)

Read this first, then [`spike-verification-token-flow.md`](spike-verification-token-flow.md) for the
findings behind the tokenised-link design, and
[`MILESTONE-MFA-self-governed-2fa.md`](MILESTONE-MFA-self-governed-2fa.md) for the wider milestone.

---

## 1. What is done

### 1.1 Slice 1 — tokenised links (merged as #1689)

Email confirmation and password reset links carry **single-use, time-limited tokens** instead of the
user's persistent identifier. Confirmation no longer mutates state on GET.

| Task | State |
|---|---|
| [#1678](https://github.com/qbicsoftware/data-manager-app/issues/1678) tokenised confirmation | ✅ merged |
| [#1679](https://github.com/qbicsoftware/data-manager-app/issues/1679) confirmation off `/login` | ✅ merged |
| [#1680](https://github.com/qbicsoftware/data-manager-app/issues/1680) tokenised reset | ✅ merged |
| [#1684](https://github.com/qbicsoftware/data-manager-app/issues/1684) migration guide + config | ✅ merged |

**Schema:** `user_verification_token` — in `complete-schema.sql`, runbook in
[`docs/migrations/NEXT.md`](../migrations/NEXT.md) (Migration #5).

> The four issues above are **still open** on GitHub even though the PR said `Closes:` them. Close
> them by hand when #1690 merges, or they will sit in the milestone forever.

### 1.2 Slice 2 — uniform failures (#1681) — in #1690, code-only, no sign-off needed

| Change | Why it was not just the issue's one bullet |
|---|---|
| `UserDetailsServiceImpl` no longer passes a `null` stored password | a `null` made `PasswordEncryptionPolicy` throw NPE → **unhandled 500**, a *third* outcome distinguishable from both "wrong password" and "no account", for exactly the ORCID-only accounts. Replaced with a well-formed `NO_PASSWORD` stand-in |
| `IdentityService.requestPasswordReset` returns success for unknown **and** unconfirmed accounts | it used to return `UserNotFoundException` / `UserNotActivatedException`, which the view rendered verbatim |
| `ResetPasswordComponent` lost its account-existence field validator | **#1680 did not reach the form.** The submit path was uniform, but the form still answered "No user with the provided mail address is known." as a repeatable server-side check, one step earlier |

### 1.3 Slice 2 — throttling (#1682) — in #1690, **needs §12 sign-off**

Server-side throttling for login, resend-confirmation and resend-reset. Three production call sites
(the previous attempt had zero, which is why it was removed):

- `LoginThrottleFilter` — registered before `UsernamePasswordAuthenticationFilter`, rejects **before**
  any password work;
- `AuthAttemptEventListener` — counts failures from Spring's own auth events, so every entry point is
  covered;
- `EmailConfirmationMain` / `ResetPasswordMain` — replaced the 30 s `java.util.Timer` (UI-scoped
  counter; a page reload reset it, so it was not a control).

**Tests:** 41 throttling/token specs green on **both** H2 and real MariaDB 12.3/InnoDB; 362
`identity`/infrastructure specs. CI green, including the SonarCloud quality gate.

**Schema:** `auth_attempt` — `sql/migrations/auth-attempt-throttling.sql`, **AWAITING §12 SIGN-OFF**,
not yet in a fresh-install path that operators follow (`complete-schema.sql` *is* updated, but
`NEXT.md` says do not apply to production until approved).

---

## 2. What is NOT done — the honest list

### 2.1 Registration still enumerates accounts — deliberately deferred to R2

`UserRegistrationComponent` and `UserRegistrationOrcIdComponent` still call `isEmailAvailable` in
field validation and reject a registered address with *"Email address already in use"*. This is the
**same oracle** just removed from the reset form, and it is the third bullet of #1681.

It is **not** fixed here because `FEAT-AUTH-FLOWS-04` ([#1676](https://github.com/qbicsoftware/data-manager-app/issues/1676), R2)
rewrites both components into one. Fixing it twice means writing the removal into a file the next
release replaces.

⚠️ **If R2 slips, this is a live `AUTH-NFR-03` violation.** Do not close the requirement until it is
fixed. The fix itself is mechanical: drop the `isEmailAvailable` validator, and make
`registerUser` return success for an address that is already registered while notifying the existing
owner by mail. The second half is **not** mechanical — `registerUser` currently returns
`UserExistsException`, and `UserRegistrationMain` surfaces it. Both need changing together, or the
disclosure just moves from the form to the submit response.

### 2.2 Origin-scope throttling blocked

`AUTH-NFR-03` asks for **per account and per origin**. Only account scope ships. The app is behind
HAProxy with `server.forward-headers-strategy` unset, nothing reads `getRemoteAddr()`, so the client
address is the proxy — an origin bucket would be global and one attacker could block everyone.

Needs: trusted forwarded headers (trusted-proxy allowlist + HAProxy **overwriting** not appending
`X-Forwarded-For`). That also unblocks passkeys and the ORCID `{baseUrl}` redirect — milestone plan
§1a condition 1. **No schema change needed**; `scope` is an open varchar.

### 2.3 ORCID-only accounts still have no password

[#1683](https://github.com/qbicsoftware/data-manager-app/issues/1683) — untouched. `User.createOidc()`
still passes `null`, so ORCID-only accounts remain passwordless. They can no longer *crash* the login
(`NO_PASSWORD` fixed that), but they cannot log in either. Runbook drafted in `NEXT.md`; three
questions unsettled there: who triggers the invitation job, how double-issue is prevented, and what
happens when the mailbox is dead (the account must **not** be deleted — an accepted exception, not a
silent failure).

### 2.4 R2 consolidation not started

[#1676](https://github.com/qbicsoftware/data-manager-app/issues/1676) — one registration form, one
password policy, display name no longer a credential. Deferred (decision B5) so the security fix
stays revertable on its own.

### 2.5 Throttle retention not scheduled

Rows in `auth_attempt` are never pruned. `JpaAuthAttemptCounter.purgeOlderThan(Instant)` exists and
`lastAttemptAt` is indexed, but the job is **not scheduled**: retention is a compliance decision owned
by [#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669). The table grows unbounded
until that lands. Small, but unbounded, and recorded rather than left to be discovered.

---

## 3. Open decisions waiting on a human

| # | Decision | Why it matters |
|---|---|---|
| 1 | **§12 sign-off for `auth_attempt`** | Blocks #1690. The DDL is validated on MariaDB 12.3 (idempotent, rolls back, upsert verified) but approval is a human act. |
| 2 | **GitGuardian dismissal on #1689** | Moot if the findings were dismissed there; if not, they reappear on #1690 for the same reason (the key name `…set-password-lifetime` matches a *Generic Password* regex; value is `PT1H`, an ISO-8601 duration). A security-tool override, not an agent decision. |
| 3 | **Startup schema guard** | Should the app **refuse to boot** if `user_verification_token` or `auth_attempt` is missing, or log a hard ERROR and stay up? Today: blank page, or every login failing with a SQL error. Refusing to boot is honest but turns a missed migration into an outage. **This decision is now more pressing**: a missing `auth_attempt` breaks *login itself*, which is worse than the blank page that motivated the question. |
| 4 | **`spring-tx` in `identity`** | Added for `@Transactional` on token consumption. Follows `project-management`'s pattern, but it is a new dependency in a module that had none. |
| 5 | **Vaadin-native confirm button vs. real POST** | `ConfirmEmailMain` renders a button and mutates on click, so a scanner cannot confirm — but the click is a Vaadin round-trip, not a form POST. Satisfies `AUTH-R-05` in substance, not in shape. |

---

## 4. Known gaps and risks

- **Upgrade path unverified for both tables.** Neither DDL has been applied to a populated copy of a
  real database. `user_verification_token` has an FK against `users` (collation, column type)
  that is unverified — a 30-second check would close it: `mysqldump --no-data` the test instance into
  a scratch DB, apply the migration twice, inspect. `auth_attempt` has no FK, so it is lower risk.
- **`#1573` is unrelated** — it deletes expired *personal access* tokens per user, not verification
  tokens.
- **ADR 0011 is still missing** ([#1667](https://github.com/qbicsoftware/data-manager-app/issues/1667))
  — `docs/adr/0011-*.md` does not exist though `docs/adr/README.md` is the index. Not blocking R1;
  blocking M2.
- **`datamanager-app` has 139 pre-existing spec errors in the sandbox** — all
  `MockitoMockMaker could not be instantiated`. Verified identical on the untouched baseline, so it is
  environmental. The auth specs must be run **by name** locally; CI's full `test` job is the real gate.
- **`scripts/run-verification-spike.sh h2` does not work** — the `h2` argument falls through to Maven
  as a lifecycle phase. Run it with **no argument** for H2, or `mariadb` for MariaDB.

---

## 5. How to work on this

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current   # not on the default PATH here

# fast feedback, H2 in MySQL mode — NO argument; "h2" is passed through to Maven and fails
./scripts/run-verification-spike.sh

# the one that matters: real InnoDB row locking and upsert semantics
./scripts/run-verification-spike.sh mariadb

# the auth specs by name (the full module run hits the sandbox Mockito issue)
./mvnw -o -pl datamanager-app -am test \
  -Dtest='UserDetailsServiceImplSpec,LoginThrottleWiringSpec,ResetPasswordMainSpec,ResetPasswordComponentAccountOracleSpec,SetNewPasswordMainSpec,ConfirmEmailMainSpec,LoginLayoutSpec,AuthAttempt*Spec,VerificationToken*Spec' \
  -Dsurefire.failIfNoSpecifiedTests=false -DskipITs
```

**Gotchas that cost time:**
- `-pl datamanager-app` **without** `-am` resolves a stale jar from `~/.m2` → confusing
  `cannot find symbol` errors. Almost always `-am` is what you want.
- Stale Groovy stubs survive a normal build; after deleting or renaming a test class,
  `rm -rf datamanager-app/target/generated-sources`.
- `spring-test` is **not** on the test classpath, so `MockHttpServletRequest` / `MockFilterChain` are
  unavailable — hand-rolled doubles are used in `LoginThrottleWiringSpec`.
- Spock mocks cannot be created in `static` scope. Helper methods that build stubs must be instance
  methods.
- `OncePerRequestFilter.doFilter` needs container bookkeeping; specs call `doFilterInternal`.
- `BeforeEvent` and concrete `@Service` classes like `EmailConfirmationLinkSupplier` cannot be mocked
  (no Objenesis, no no-arg constructor). Use a real `BeforeEnterEvent` with a `Router` stub, and
  construct the real service with literal arguments.
- **Stale Groovy stubs** show up as `NoClassDefFoundError …$_closure1` after editing a spec's inner
  classes. `rm -rf {datamanager-app,identity,identity-infrastructure}/target/generated-sources`, then
  rerun.
- Vaadin specs need `VaadinService.setCurrent(...)` with a router stub, because router links resolve
  their href at construction.

---

## 6. Where the important knowledge lives

| Question | File |
|---|---|
| Why only a hash is stored, why SHA-256 and not the PAT encoder | header of `sql/migrations/auth-flow-hardening.sql` |
| Why consumption must be one transaction; the 8-of-8 failure | `docs/plans/spike-verification-token-flow.md` |
| Why reissue deadlocked and leaked live tokens | *Finding 6* in the same file |
| **Why the throttle deadlocked on MariaDB but not H2** | header of `sql/migrations/auth-attempt-throttling.sql` + `NEXT.md` Migration #6 |
| **Why the login path must NOT add its own dummy derivation** | `UserDetailsServiceImpl` class javadoc — Spring already does exactly one, and a second would create the timing oracle |
| Operator steps, link-format breakage, threshold properties | `docs/migrations/NEXT.md` (Migrations #5 and #6) |
| Deployment blueprint (literal values, no env vars) | `application.properties.template` |
| Design rules for the token aggregate | `VerificationToken` javadoc + `VerificationTokenSpec` |

---

## 7. Suggested next steps, in order

1. **Get §12 sign-off for `auth_attempt`, then merge #1690.**
2. **Close #1681, #1682** (delivered by #1690) **and #1678, #1679, #1680, #1684** (delivered by #1689
   but never closed).
3. **Decide the startup schema guard** (open decision 3). It now matters more: a missing
   `auth_attempt` breaks login rather than showing a blank page.
4. **`#1667` ADR 0011** before any M2 (second-factor) work begins — the milestone gate requires it.
5. **`#1676` R2** — and with it the registration enumeration fix in §2.1, which is a live requirement
   violation until then.
6. **`#1683` ORCID passwords** — settle the three open questions in `NEXT.md` first.

---

## 8. Lessons worth carrying forward

### From slice 1

Of the four bugs fixed there, **three were found by the PO running the application**, not by the test
suite: the reset flow never being migrated, the reissue deadlock that left multiple live tokens, and a
blank page caused by inventing CSS class names that did not exist in the theme. Every one of those
passed a green test suite.

- **Check the theme/asset side against your own class names** — `grep` the CSS before trusting a class
  name; one matched only as a *substring* of the real one.
- **Exercise the route, not just the service.**

### From slice 2 — the same lesson, three more times

1. **The obvious fix would have created the bug.** #1681 asks for a dummy password comparison for
   unknown accounts. Spring Security already does exactly that, one layer down. Adding it in
   `UserDetailsServiceImpl` would have made an unknown address cost **twice** a wrong password —
   turning the class into the timing oracle it was meant to remove. *Read the framework before
   "fixing" what it already handles.*

2. **Fixing the endpoint is not fixing the feature.** #1680 made the reset *response* uniform; the
   reset *form* went on enumerating accounts as field validation, one step earlier, for months. The
   second disclosure was invisible to a spec that only exercised `resetPassword(String)`. *When a
   requirement says "any response to an unauthenticated request", it includes the ones you did not
   write by hand.*

3. **H2 passing is not evidence.** The throttle upsert deadlocked on real InnoDB at 16 concurrent
   attempts and passed on H2. This is the **third** time this project has hit that: the token
   consumption race and the reissue deadlock were the same shape. *If the change touches locking,
   uniqueness, or atomicity, the MariaDB run is the test — the H2 run is just the fast one.*

4. **The service was right and the wiring was wrong — again.** Covering `EmailConfirmationMain` (the
   last uncovered file, at 0%) immediately found a real bug in *both* resend call sites:

   ```java
   if (!throttle.isThrottled(...)) { throttle.record(...); doWork(); }
   ```

   `record()` returns `THROTTLED` when the attempt *itself* crosses the threshold, but the value was
   discarded — so the request that tripped the limit still did the work, and the limit was off by one.
   The throttle's own 18 specs never caught it, because they consume the return value. *A control
   tested only at the service layer is not tested.*

**Corollary for the next session:** when a fix "works" in a spec but not in the app, suspect the
wiring, the theme, and the transaction boundary — in that order. When a *security* control works,
check first that anything actually calls it.
