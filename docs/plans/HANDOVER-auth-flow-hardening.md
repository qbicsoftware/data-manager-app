# Handover — FEAT-AUTH-FLOWS R1 (auth-flow hardening)

**Written:** 2026-11-11 · **Branch:** `feature/auth-flow-hardening` · **PR:** [#1689](https://github.com/qbicsoftware/data-manager-app/pull/1689) (ready for review, not merged)

Read this first, then [`spike-verification-token-flow.md`](spike-verification-token-flow.md) for the
findings behind the design, and [`MILESTONE-MFA-self-governed-2fa.md`](MILESTONE-MFA-self-governed-2fa.md)
for the wider milestone this belongs to.

---

## 1. What is done

Email confirmation and password reset links now carry **single-use, time-limited tokens** instead of
the user's persistent identifier. Confirmation no longer mutates state on GET. The reset form no
longer discloses whether an account exists.

| Task | State |
|---|---|
| [#1678](https://github.com/qbicsoftware/data-manager-app/issues/1678) tokenised confirmation | ✅ done, closes with this PR |
| [#1679](https://github.com/qbicsoftware/data-manager-app/issues/1679) confirmation off `/login` | ✅ done |
| [#1680](https://github.com/qbicsoftware/data-manager-app/issues/1680) tokenised reset | ✅ done |
| [#1684](https://github.com/qbicsoftware/data-manager-app/issues/1684) migration guide + config | ✅ done |

**Verified:** 339 identity/infra specs, 65 app specs, and the 30 spike specs on **both** H2 and
MariaDB 12.3 (the MariaDB run is the one that proves InnoDB row locking; H2's lock semantics are not
a substitute). Three
regression guards were each proven to fail when the fix is reverted (deadlock, supersede-on-change,
expiry boundary). Manually verified by the PO on a running instance: confirmation works once,
reset works once, registration end-to-end.

**Schema:** `user_verification_token` — promoted out of `drafts/`, added to `complete-schema.sql`,
operator runbook in [`docs/migrations/NEXT.md`](../migrations/NEXT.md).

---

## 2. What is NOT done — the honest list

### 2.1 Throttling does not exist (blocks closing AUTH-NFR-03)

[#1682](https://github.com/qbicsoftware/data-manager-app/issues/1682) is **unimplemented**. An
`AuthAttemptThrottle` was written and then **removed from this PR** because it had zero production
callers — dead code dressed as a security control. There is currently **no server-side rate limit** on
login, confirmation resend, or reset requests.

Two non-obvious requirements, so they are not rediscovered:
- the counter must be an **upsert** (`INSERT … ON DUPLICATE KEY UPDATE`) — read-then-write loses
  increments and hands an attacker a free attempt per race;
- it must have **no FK to `users`**, or the throttle itself becomes an account-enumeration oracle.

Origin-scope throttling is **blocked**: `server.forward-headers-strategy` is unset and nothing reads
`getRemoteAddr()`, so the client address is HAProxy. Keying a bucket on it would let one attacker
block the whole user population.

### 2.2 Uniform login failures not done

[#1681](https://github.com/qbicsoftware/data-manager-app/issues/1681) — `UserDetailsServiceImpl`
(`datamanager-app/.../security/`) still does
`userInformationService.findByEmail(mail).orElseThrow(() -> new UsernameNotFoundException("Cannot find user"))`
**before** any password verification, so an unknown account is rejected earlier and differently from a
wrong password.

### 2.3 ORCID-only accounts still have no password

[#1683](https://github.com/qbicsoftware/data-manager-app/issues/1683) — untouched. `User.createOidc()`
still passes `null` for the encrypted password, so ORCID-only accounts remain passwordless. Runbook
drafted in `NEXT.md`; three questions are unsettled and recorded there: who triggers the invitation
job, how double-issue is prevented, and what happens when the mailbox is dead (the account must
**not** be deleted, so it stays passwordless — an accepted exception, not a silent failure).

### 2.4 R2 consolidation not started

[#1676](https://github.com/qbicsoftware/data-manager-app/issues/1676) — one registration form, one
password policy, display name no longer a credential. Deliberately deferred (decision B5) so the
security fix stays revertable on its own.

---

## 3. Open decisions waiting on a human

| # | Decision | Why it matters |
|---|---|---|
| 1 | **GitGuardian dismissal** | 2 findings on #1689 are false positives (the key name `…set-password-lifetime` matches a *Generic Password* regex; the value is `PT1H`, an ISO-8601 duration). Needs a dismissal with that justification — a security-tool override, so not an agent decision. |
| 2 | **Startup schema guard** | Should the app **refuse to boot** if `user_verification_token` is missing, or log a hard ERROR and stay up? Today the failure mode is a blank page. Refusing to boot is honest but turns a missed migration into an outage. |
| 3 | **`spring-tx` in `identity`** | Added for `@Transactional` on consumption. Follows `project-management`'s pattern, but it is a new dependency in a module that had none. Flagged for review. |
| 4 | **Vaadin-native confirm button vs. real POST** | `ConfirmEmailMain` renders a button and mutates on click, so a scanner cannot confirm — but the click is a Vaadin round-trip, not a form POST to the route. Satisfies AUTH-R-05 in substance, not in shape. |

---

## 4. Known gaps and risks

- **Upgrade path unverified.** The DDL was validated against *throwaway* MariaDB instances with
  synthetic schemas. It was never applied to a populated copy of a real database, so the FK against an
  existing `users` table (collation, column type) is unverified. A 30-second check would close it —
  `mysqldump --no-data` the test instance into a scratch DB, apply the migration twice, inspect.
- **`#1573` is unrelated** — it deletes expired *personal access* tokens per user, not verification
  tokens. No overlap with the purge job `NEXT.md` leaves to
  [#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669); that job still needs writing
  from scratch.
- **Retention is undecided** ([#1669](https://github.com/qbicsoftware/data-manager-app/issues/1669)).
  Do **not** implement a purge job yet; the pre-rollback export may be the only copy of the audit trail.
- **ADR 0011 is still missing** ([#1667](https://github.com/qbicsoftware/data-manager-app/issues/1667))
  — `docs/adr/0011-*.md` does not exist though `docs/adr/README.md` is the index. Not blocking R1;
  blocking M2.
- **Branch history contains a closed PR lineage** (#1687 superseded, #1688 auto-closed by a branch
  rename). Harmless, documented in the PR body.

---

## 5. How to work on this

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current   # not on the default PATH here

# fast feedback, H2 in MySQL mode (no setup)
./scripts/run-verification-spike.sh

# the one that matters: real InnoDB row locking, bootstraps and tears down MariaDB
./scripts/run-verification-spike.sh mariadb

# the specs are also runnable directly
./mvnw -o -pl datamanager-app -am test -Dtest='VerificationToken*Spec' \
  -Dsurefire.failIfNoSpecifiedTests=false -DskipITs
```

**Gotchas that cost time:**
- `-pl datamanager-app` **without** `-am` resolves a stale jar from `~/.m2` and produces confusing
  `cannot find symbol` errors. Almost always `-am` is what you want.
- Stale Groovy stubs survive a normal build; after deleting or renaming a test class, `rm -rf
  datamanager-app/target/generated-sources`.
- Vaadin won't boot in some sandboxes (`~/.vaadin` not creatable). The specs don't need it; a real
  boot does.
- Specs are parameterised for the engine: `-Dverification.jdbc.url/.user/.password/.driver/.dialect`.
  H2 infers its dialect; MariaDB needs `-Dverification.dialect=org.hibernate.dialect.MariaDBDialect`.

---

## 6. Where the important knowledge lives

| Question | File |
|---|---|
| Why only a hash is stored, why SHA-256 and not the PAT encoder | header of `sql/migrations/auth-flow-hardening.sql` |
| Why consumption must be one transaction; the 8-of-8 failure | `docs/plans/spike-verification-token-flow.md` |
| Why reissue deadlocked and leaked live tokens | section *Finding 6* in the same file |
| Why throttling was removed rather than shipped | same file, *Deferred to #1682* |
| Operator steps, link-format breakage, config migration | `docs/migrations/NEXT.md` |
| Deployment blueprint (literal values, no env vars) | `application.properties.template` |
| Design rules for the token aggregate | `VerificationToken` javadoc + `VerificationTokenSpec` |

---

## 7. Suggested next steps, in order

1. **Merge #1689** once CI is green and the GitGuardian findings are dismissed.
2. **Close #1678, #1679, #1680, #1684** (all delivered by that PR).
3. **`#1682` throttling** — the only AUTH-NFR-03 clause still unmet. Start with the two constraints in
   §2.1; they are the ones that cause rework if missed.
4. **`#1681` uniform login failures** — small, and #1689 already removed the equivalent defect from the
   reset form, so the pattern to copy exists.
5. **`#1667` ADR 0011** before any M2 (second-factor) work begins — the milestone gate requires it.
6. **`#1683` ORCID passwords** — settle the three open questions in `NEXT.md` first.

---

## 8. One lesson worth carrying forward

Of the four bugs fixed in this slice, **three were found by the PO running the application**, not by
the test suite: the reset flow never being migrated, the reissue deadlock that left multiple live
tokens, and a blank page caused by inventing CSS class names that did not exist in the theme.

Every one of those passed a green test suite. The specs asserted what the code did; none asserted what
a user would *see*. Two habits would have caught them earlier:

- **check the theme/asset side against your own class names** — `grep` the CSS before trusting a
  class name; mine matched only as a *substring* of the real one;
- **exercise the route, not just the service** — the features were verified at the service layer while
  the routes were broken.

Corollary for the next session: when a fix "works" in a spec but not in the app, suspect the wiring,
the theme, and the transaction boundary — in that order.
