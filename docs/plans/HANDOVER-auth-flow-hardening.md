# Handover — FEAT-AUTH-FLOWS R1 (auth-flow hardening)

**Written:** 2026-11-11 · **Branch:** `feature/auth-throttling-and-uniform-failures` ·
**PR:** [#1690](https://github.com/qbicsoftware/data-manager-app/pull/1690) (open, awaiting §12 sign-off
on the throttling table)

Read this first, then [`spike-verification-token-flow.md`](spike-verification-token-flow.md) for the
findings behind the tokenised-link design, and
[`MILESTONE-MFA-self-governed-2fa.md`](MILESTONE-MFA-self-governed-2fa.md) for the wider milestone.

---

## 1. What is done

| Slice | Delivered by | State |
|---|---|---|
| Tokenised confirmation / reset links | #1689 (merged) | ✅ |
| Uniform failures (`AUTH-NFR-03`) | #1690 (merged) | ✅ |
| Server-side throttling, account scope | #1690 (merged) | ✅ code shipped · ✅ §12 sign-off recorded |

Email confirmation and password reset links carry **single-use, time-limited tokens** instead of the
user's persistent identifier. Confirmation no longer mutates state on GET.

| Task | State |
|---|---|
| Ops-doc manual prune no longer lifts an active block | ✅ fixed |
| `NEXT.md` #5 vs. #6 contradiction ("throttling is not in this release") | ✅ fixed |
| `NEXT.md` #5 self-description ("Two tables") after the split | ✅ fixed |
| #1678, #1679, #1680, #1681, #1682 closed | ✅ |
| #1684 (migration guide) closed | ✅ |
| Auth specs re-run on `origin/development` | ✅ 107 passed, 0 failed |
| Handover §7.3 "unknown address four times" | ✅ already pinned by spec, see §7 |
| **§12 sign-off for `auth_attempt`** | ✅ **recorded by the Product Owner** — see #1690 |

**Schema:** `user_verification_token` — in `complete-schema.sql`, runbook in
[`docs/migrations/NEXT.md`](../migrations/NEXT.md) (Migration #5).

1. ~~A human records the AGENTS.md §12 sign-off for `auth_attempt`~~ — recorded by the Product Owner on
   #1690, and the script header and `NEXT.md` now say so.
2. **On the test server:** `DROP TABLE IF EXISTS auth_attempt;` → apply
   `sql/migrations/auth-attempt-throttling.sql` → `SHOW CREATE TABLE auth_attempt\G` must contain
   **`windowSeconds`**. Then follow the smoke test in the ops doc §2.
3. **Run the application** — see §2. The specs are not the evidence that matters.

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
| 1 | ~~**§12 sign-off for `auth_attempt`**~~ | ✅ recorded |
| 2 | **ADR 0011** ([#1667](https://github.com/qbicsoftware/data-manager-app/issues/1667)) | milestone gate for any M2 work; `docs/adr/0011-*.md` does not exist |
| 3 | **Startup schema guard** | should the app refuse to boot if `user_verification_token` / `auth_attempt` is missing? A missing `auth_attempt` **breaks login outright**, which is worse than the blank page that motivated the question |
| 4 | **HAProxy `filter cache` on app paths** | currently inert — Spring Security sends `no-store` and HAProxy honours it — but the protection is accidental and lives in framework defaults. With `alpn h2` + connection coalescing there is no `Vary: Cookie`. Scope the cache to `/VAADIN/` or remove it. Recorded in #1691 |
| 5 | **`server.servlet.session.cookie.domain`** | set to `de.uni-tuebingen.qbic`, **not a suffix** of `rdm.qbic.uni-tuebingen.de`, so browsers drop it. Harmless but it will mislead the next debugger |
| 6 | **Throttle thresholds** | login 10/15 min confirmed by PO. The 15-min *block* will generate support tickets (a legitimate user can be locked out); one env var to tune |

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

1. ~~Merge #1690 and close #1678–#1684~~ — done. ~~The §12 sign-off is still outstanding~~ — recorded.
2. **Verify on the test server**: missing `windowSeconds` was the last trap, and the smoke test in ops §2
   covers it.
3. ~~Confirm the "unknown address four times" claim~~ — **already covered, do not re-derive.**
   `ResetPasswordMainSpec."the throttled reply is identical for a registered and an unregistered
   address"` submits four times to both and asserts the rendered panel is character-for-character
   equal; `AuthAttemptThrottleSpec."an unknown address is throttled exactly like a known one"` pins the
   store-level property. What remains genuinely unverified is the **live app** (see §2).
4. Apply the migration on the test server and run the ops §2 smoke test. The DDL must precede the code
   deploy — a missing `auth_attempt` fails login outright.
5. **`#1667` ADR 0011** — still open, and the milestone's M0 gate names it as a prerequisite for *all*
   code. R1 shipped anyway; decide whether the gate still binds before R2.
6. **`#1676` R2** — and with it the registration enumeration fix (§3.3), a live requirement violation.
7. **`#1683` ORCID passwords** — settle the three questions in `NEXT.md` first.
8. **Scheduled `auth_attempt` purge** — deliberately not built. `purgeOlderThan` exists and is tested
   but unscheduled; retention is the compliance number owned by #1669, and "purge after N days" is
   arguably a new capability that needs a requirement before it becomes a Task. Recorded on #1669, not
   invented here.

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
