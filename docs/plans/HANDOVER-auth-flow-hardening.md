# Handover — FEAT-AUTH-FLOWS R1 (auth-flow hardening)

**Written:** 2026-11-11 · **Updated:** 2026-11-12 (R1 closeout) · **Branch:** R1 is merged; closeout on
`chore/auth-r1-closeout` ·
**PR:** [#1690](https://github.com/qbicsoftware/data-manager-app/pull/1690) — **MERGED** as `500da2faf`

Read this first, then [`spike-verification-token-flow.md`](spike-verification-token-flow.md) for the
tokenised-link findings, [`MILESTONE-MFA-self-governed-2fa.md`](MILESTONE-MFA-self-governed-2fa.md) for
the milestone, and [`docs/operations/installation-and-administration.md`](../operations/installation-and-administration.md)
for the deployment contract.

---

## 1. State of play

| Slice | Delivered by | State |
|---|---|---|
| Tokenised confirmation / reset links | #1689 (merged) | ✅ |
| Uniform failures (`AUTH-NFR-03`) | #1690 (merged) | ✅ |
| Server-side throttling, account scope | #1690 (merged) | ✅ code shipped · ⚠️ **§12 sign-off never recorded** |

> ⚠️ **This file was stale on `development`.** The accurate revision above lived only on
> `feature/auth-throttling-and-uniform-failures` and was never merged; `development` still held the
> pre-merge draft that said "awaiting merge". Restored during the R1 closeout. When a handover is
> rewritten in the last commit of a branch, merge that commit — squashing drops it.

### 1.1 What the closeout changed

| Item | State |
|---|---|
| Ops-doc manual prune no longer lifts an active block | ✅ fixed |
| `NEXT.md` #5 vs. #6 contradiction ("throttling is not in this release") | ✅ fixed |
| `NEXT.md` #5 self-description ("Two tables") after the split | ✅ fixed |
| #1678, #1679, #1680, #1681, #1682 closed | ✅ |
| #1684 (migration guide) closed | ✅ |
| Auth specs re-run on `origin/development` | ✅ 107 passed, 0 failed |
| Handover §7.3 "unknown address four times" | ✅ already pinned by spec, see §7 |
| **§12 sign-off for `auth_attempt`** | ⛔ **still not recorded — do not apply to production** |

### 1.2 Still to do before the migration reaches a real server

1. **A human records the AGENTS.md §12 sign-off** for `auth_attempt` on #1690 or #1682. Until then the
   script header keeps `AWAITING SIGN-OFF` and honest operators will refuse to run it.
2. **On the test server:** `DROP TABLE IF EXISTS auth_attempt;` → apply
   `sql/migrations/auth-attempt-throttling.sql` → `SHOW CREATE TABLE auth_attempt\G` must contain
   **`windowSeconds`**. Then follow the smoke test in the ops doc §2.
3. **Run the application** — see §2. The specs are not the evidence that matters.

---

## 2. The most important thing in this file

**Six production bugs surfaced after the test suite was green. Every one was a wiring defect, and every
one was found by the PO running the application, not by a spec.**

| # | Bug | Why the specs could not see it |
|---|---|---|
| 1 | Bean cycle → app would not boot | no `@SpringBootTest` in the module |
| 2 | No `@Transactional` → 500 on every failed login | every spec supplied its own transaction |
| 3 | Success cleared a UUID-keyed bucket | spec stubbed `getName()` to the email |
| 4 | Sliding window → counter stuck at 1 | every spec passed a **constant** `now` |
| 5 | `@UIScope` listener stacking → double counting | every spec built the view **once** |
| 6 | Throttle hidden in the UI | the spec asserted the buggy behaviour |

All six now have guards, each proven by reinstating its bug. **But the pattern will recur**, so when the
next change "works in a spec but not in the app", check these in order:

1. **Wiring** — is anything actually calling it? Does the bean resolve?
2. **Transaction and event boundaries** — is there an active transaction on the write path?
3. **The clock and the session** — does the code see the same `now` and the same object instance
   production does?
4. **The view and the theme** — does the route render, with class names that exist in the CSS?

And specifically for this codebase:

- **A spec that supplies something production does not** (a transaction, a stub shaped differently, a
  constant clock, a single view construction) **proves nothing about production.**
- **A green run after a deliberate break only means something if the break is faithful.** Reinstating one
  half of a two-part bug passes. That happened here.

---

## 3. What is NOT done

### 3.1 Origin-scope throttling — blocked, tracked, not lost

`AUTH-NFR-03` asks for per-account **and** per-origin. Only account scope ships.

- **#1691** · milestone `MILESTONE-PROXY` — the staged plan (proxy header → canary → origin buckets)
- Blocked because the client address is the HAProxy: `forward-headers-strategy` unset, nothing reads
  `getRemoteAddr()`

**Verified finding, do not re-derive:** `ForwardedHeaderFilter` (`framework`) takes the **leftmost**
`X-Forwarded-For` with no trust check; `RemoteIpValve` (`native`) only trusts the header when the
connecting peer is in `internal-proxies`, then walks right-to-left discarding client-supplied values.
**Use `native`.** `option forwardfor` alone appends, which would let a client pick its own bucket.

**Residual risk until then:** anyone who knows a victim's address can lock them out for the block
duration (15 min default).

### 3.2 Throttle retention — no purge job

`JpaAuthAttemptCounter.purgeOlderThan` exists and is tested, but **nothing schedules it**. Retention is a
privacy decision owned by [#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669):
`windowStartAt` is closer to an activity log than a counter, since the hash of a *known* address is
computable.

**Small follow-up worth doing:** the manual prune in the ops doc §3 should exclude rows with an active
block, or it silently unblocks an address mid-block:

```sql
DELETE FROM auth_attempt
 WHERE lastAttemptAt < UTC_TIMESTAMP() - INTERVAL 30 DAY
   AND (blockedUntil IS NULL OR blockedUntil < UTC_TIMESTAMP());
```

### 3.3 Registration still enumerates accounts

`UserRegistrationComponent` / `UserRegistrationOrcIdComponent` call `isEmailAvailable` in field
validation — **the same oracle just removed from the reset form**. Deliberately deferred to
`FEAT-AUTH-FLOWS-04` ([#1676](https://github.com/qbicsoftware/data-manager-app/issues/1676), R2), which
rewrites both components.

⚠️ **This is a live `AUTH-NFR-03` violation until R2 ships.** And the fix is not purely mechanical:
`registerUser` returns `UserExistsException` and the view renders it — both must change together, or the
disclosure just moves from the form to the submit response.

### 3.4 ORCID-only accounts still have no password

[#1683](https://github.com/qbicsoftware/data-manager-app/issues/1683). They can no longer *crash* the
login (`NO_PASSWORD` fixed that), but they cannot log in either. Three questions unsettled in `NEXT.md`.

### 3.5 R2 consolidation not started

[#1676](https://github.com/qbicsoftware/data-manager-app/issues/1676) — one registration form, one
password policy, display name no longer a credential.

---

## 4. Open decisions waiting on a human

| # | Decision | Why it matters |
|---|---|---|
| 1 | **§12 sign-off for `auth_attempt`** | blocks #1690 |
| 2 | **ADR 0011** ([#1667](https://github.com/qbicsoftware/data-manager-app/issues/1667)) | milestone gate for any M2 work; `docs/adr/0011-*.md` does not exist |
| 3 | **Startup schema guard** | should the app refuse to boot if `user_verification_token` / `auth_attempt` is missing? A missing `auth_attempt` **breaks login outright**, which is worse than the blank page that motivated the question |
| 4 | **HAProxy `filter cache` on app paths** | currently inert — Spring Security sends `no-store` and HAProxy honours it — but the protection is accidental and lives in framework defaults. With `alpn h2` + connection coalescing there is no `Vary: Cookie`. Scope the cache to `/VAADIN/` or remove it. Recorded in #1691 |
| 5 | **`server.servlet.session.cookie.domain`** | set to `de.uni-tuebingen.qbic`, **not a suffix** of `rdm.qbic.uni-tuebingen.de`, so browsers drop it. Harmless but it will mislead the next debugger |
| 6 | **Throttle thresholds** | login 10/15 min confirmed by PO. The 15-min *block* will generate support tickets (a legitimate user can be locked out); one env var to tune |

---

## 5. How to work on this

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current   # not on the default PATH

# H2 in MySQL mode — NO argument; "h2" is passed to Maven as a lifecycle phase and fails
./scripts/run-verification-spike.sh

# the one that matters: real InnoDB semantics
./scripts/run-verification-spike.sh mariadb

# auth specs by name — the full module run hits a sandbox Mockito issue (139 pre-existing errors,
# verified identical on the untouched baseline)
./mvnw -o -pl datamanager-app -am test \
  -Dtest='ResetSubmitOnceSpec,ResetPasswordMainSpec,EmailConfirmationResendSpec,LoginThrottleWiringSpec,UserDetailsServiceImplSpec,AuthAttempt*Spec,VerificationToken*Spec,ResetPasswordComponentAccountOracleSpec,SecurityConfigurationBeanCycleSpec' \
  -Dsurefire.failIfNoSpecifiedTests=false -DskipITs
```

**Gotchas that cost time:**

- `-pl datamanager-app` **without** `-am` resolves a stale jar from `~/.m2` → confusing `cannot find
  symbol`. Almost always `-am`.
- **Stale Groovy stubs** → `NoClassDefFoundError …$_closure1`. `rm -rf
  {datamanager-app,identity,identity-infrastructure}/target/generated-sources`.
- Spock mocks **cannot be created in `static` scope** — helper methods building stubs must be instance
  methods.
- `spring-test` is **not** on the classpath: no `MockHttpServletRequest`/`MockFilterChain`.
- `BeforeEvent` and concrete `@Service` classes cannot be mocked (no Objenesis). Use a real
  `BeforeEnterEvent` with a `Router` stub; construct real services with literal arguments.
- `OncePerRequestFilter.doFilter` needs container bookkeeping — specs call `doFilterInternal`.
- **Groovy GString trap:** `map.remove("$a|$b")` builds a `GString`, which is **not equal** to a `String`
  key. It silently removes nothing. Use a `bucketId()` helper returning a real `String`.

---

## 6. Where the important knowledge lives

| Question | File |
|---|---|
| Why only a hash is stored; SHA-256 not the PAT encoder | header of `sql/migrations/auth-flow-hardening.sql` |
| Why consumption must be one transaction; the 8-of-8 failure | `docs/plans/spike-verification-token-flow.md` |
| Why reissue deadlocked and leaked live tokens | *Finding 6*, same file |
| **Why the throttle must not add its own dummy derivation** | `UserDetailsServiceImpl` javadoc — Spring already does exactly one |
| **Why the window length is stored per row** | header of `sql/migrations/auth-attempt-throttling.sql` |
| **Why the reset page states the throttle but the confirmation page… also does** | `ResetPasswordMain` and `EmailConfirmationMain` comments — same choice, different reasons |
| Why `isThrottled` is read-only and the writes are not | `AuthAttemptThrottle` javadoc, Rule 5 |
| Proxy contract, checklist, troubleshooting, unblock SQL | `docs/operations/installation-and-administration.md` |
| Origin-scope staging plan | #1691 |

---

## 7. Suggested next steps, in order

1. ~~Merge #1690 and close #1678–#1684~~ — done. **The §12 sign-off is still outstanding**, so the
   migration must not be applied to production until a human records it on #1690 or #1682.
2. **Verify on the test server**: missing `windowSeconds` was the last trap, and the smoke test in ops §2
   covers it.
3. ~~Confirm the "unknown address four times" claim~~ — **already covered, do not re-derive.**
   `ResetPasswordMainSpec."the throttled reply is identical for a registered and an unregistered
   address"` submits four times to both and asserts the rendered panel is character-for-character
   equal; `AuthAttemptThrottleSpec."an unknown address is throttled exactly like a known one"` pins the
   store-level property. What remains genuinely unverified is the **live app** (see §2).
4. **§12 sign-off**, then apply the migration on the test server and run the ops §2 smoke test.
5. **`#1667` ADR 0011** — still open, and the milestone's M0 gate names it as a prerequisite for *all*
   code. R1 shipped anyway; decide whether the gate still binds before R2.
6. **`#1676` R2** — and with it the registration enumeration fix (§3.3), a live requirement violation.
7. **`#1683` ORCID passwords** — settle the three questions in `NEXT.md` first.
8. **Scheduled `auth_attempt` purge** — deliberately not built. `purgeOlderThan` exists and is tested
   but unscheduled; retention is the compliance number owned by #1669, and "purge after N days" is
   arguably a new capability that needs a requirement before it becomes a Task. Recorded on #1669, not
   invented here.

---

## 8. One lesson worth carrying

The specs in this slice were good. They covered concurrency, hashing, normalisation, event wiring, and
the decision logic — 100 app specs and 50 MariaDB specs, all green.

And the application still did not start, then returned 500s, then never throttled at all.

That is not an argument against the tests. It is an argument that **a test suite verifies the units you
thought about.** The defects lived in what nobody thought to vary: the number of times a view is built,
whether a transaction is open, whether two code paths agree on an identifier, whether the clock moves.

So: keep writing the specs, but **run the application before saying it works.** In this slice that was
not a formality — it was the only thing that found six bugs.
