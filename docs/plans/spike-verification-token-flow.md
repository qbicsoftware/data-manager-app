# Spike — tokenised verification flow (FEAT-AUTH-FLOWS-01)

**Branch:** `spike/verification-token-flow`
**Purpose:** answer the questions a DDL review cannot. Not production code — see *What to keep* below.
**Result:** 30 new specs, all green; 304 existing `identity` specs unaffected.

---

## What the spike set out to test

Reviewing `sql/migrations/drafts/auth-flow-hardening.sql` established that the *schema* behaves as
claimed (unique keys reject replays, no FK on `auth_attempt`, cascade works, rollback is clean). It
could not establish whether the *service layer* preserves those properties. Four questions:

1. Does a confirmation token survive the round-trip through Hibernate and get consumed exactly once?
2. Do two concurrent consumers of the same link result in one winner or two?
3. Does the throttle counter survive concurrent increments without losing any?
4. Does expiry arithmetic hold when the JVM default timezone is not UTC?

---

## 🔴 Finding 1 — consumption was NOT single-use (the reason this spike was worth running)

**The first implementation let all 8 of 8 concurrent consumers succeed.**

```
results.count { it instanceof Consumed } == 1
|       |                                |
|       8                                false
[Consumed[userId=u-1], Consumed[userId=u-1], ... 8 times]
```

Three separate mistakes combined, each invisible in a DDL review:

**1a. The unique index cannot make consumption single-use.** `uk_user_verification_token_hash`
guards **inserts**. Consumption is an **update**. The index is irrelevant to it. The DDL review I
did earlier implicitly leaned on this index as the race guard — that was wrong, and it is the kind
of error only execution surfaces.

**1b. A row lock bought nothing, because the transaction ended too early.** I had written
`findByTokenHashForUpdate` with `PESSIMISTIC_WRITE` and wired `consume()` to use it. It still failed
8/8. The reason: the lookup ran in its own transaction, so the lock was released at commit and the
entity handed to the caller was **detached**. Every thread then read a stale `consumedAt = NULL`,
passed its own check, and wrote. A lock is only meaningful inside the transaction that performs the
update.

**1c. `read → decide → write` across transactions cannot be made safe by any lock.** This is the
general lesson, and it applies to every future flow here, not just tokens. Consumption had to become
**one atomic unit of work**: lock the row, evaluate the rules, mark used, commit — all in the
adapter, driven by the aggregate's own predicates.

**Fix:** the port now exposes `consumeIfUsable(hash, purpose, now)` returning `Consumed(token)` or
`Refused(ConsumeFailure)`. The usability rules stay on the aggregate (`VerificationToken.checkConsumable`),
so the adapter owns the transaction and the lock but never re-implements the rules.

**Regression guard verified:** re-introducing the unlocked lookup makes the test fail again. The
test is a real guard, not a tautology.

**Consequence for #1678:** this must not be re-solved in the view layer. The consumption call and the
state change it authorises (confirming the address, setting the password) belong in the **same
transaction**, or a crash between them leaves the user with a dead link and an unconfirmed account.
The spike's `PerCallRepository` deliberately does one transaction per call and is **not** a model for
production.

---

## 🟠 Finding 2 — the throttle needed the same treatment, for a different reason

Read-then-write on `auth_attempt` loses increments under concurrency: two attempts both read
`attemptCount = 3`, both write `4`, and an attacker gains a free attempt per race. The counter is
therefore an upsert, so the database does the read-modify-write atomically.

Two things measured:

- **The upsert needs `H2 MODE=MySQL`.** `ON DUPLICATE KEY UPDATE` is MariaDB syntax; default H2
  rejects it outright (verified on H2 2.3.232). Without the mode flag the specs would have exercised
  a statement production never runs, and passed. Both specs now run `MODE=MySQL` and the reason is
  documented in the spec.
- **The counter's statements require a transaction.** `TransactionRequiredException` outside one.
  In production that comes from a `@Transactional` boundary on the login/reset path — which **does
  not exist yet** and must be part of #1682's task, not assumed.

The throttle's block decision stays in Java against an `Instant`; only the counter is in SQL.
Writing `blockedUntil` from SQL would bind the decision to the database session's timezone.

---

## 🟢 Finding 3 — expiry arithmetic is sound, and the test proves it

The migration flags that `expiresAt` must be compared in Java, never in SQL, because every column is
`datetime(6)` with no zone. The spec sets the JVM default to `Pacific/Kiritimati` (UTC+14) — the
largest possible offset — between issuing and consuming, and the token still consumes correctly. So
the chosen design (all `Instant`, one clock) holds. Had any code path used `NOW()` or a
`LocalDateTime`, this test would fail by up to 14 hours.

---

## 🟢 Finding 4 — normalisation is load-bearing (as predicted, now proven)

`EmailAddress` performs no trimming or case folding. The spec confirms that `alice@example.com`,
`Alice@Example.com` and `  ALICE@EXAMPLE.COM  ` all land in **one** bucket, and that the fourth
attempt against that account is blocked while only one row exists. Without normalisation these would
be three buckets and an attacker could vary capitalisation for unlimited resend mail per victim.

Normalisation uses `Locale.ROOT`, not the default locale — in a Turkish locale
`"I".toLowerCase()` yields `ı`, which would silently split the bucket.

The enumeration-oracle guard is also proven: an unknown address is throttled **identically** to a
known one (`[true, true, false]` for both), which is only possible because `auth_attempt` has no FK
to `users`.

---

## What the spike produced

| Concern | Location |
|---|---|
| `TokenPurpose`, `VerificationToken`, `VerificationTokens` (SHA-256), `ConsumeFailure` | `identity/domain/model/verification/` |
| `VerificationTokenRepository` (incl. atomic `consumeIfUsable`) | `identity/domain/repository/` |
| `VerificationTokenServiceImpl`, `AuthAttemptThrottle` | `identity/application/verification/` |
| `JpaVerificationTokenRepository`, `JpaAttemptCounter` | `identity-infrastructure/infrastructure/verification/` |
| 21 flow + 9 concurrency specs | `datamanager-app/src/test/groovy/.../verification/` |

Runtime bounds are enforced at construction, so a deployment cannot silently exceed AUTH-R-05:
confirmation ≤ 1 h, reset ≤ 15 min, set-password ≤ 1 h.

---

## What to keep vs. what to throw away

**Keep (promote into #1678):** the domain model, the atomic `consumeIfUsable` contract, the
`ConsumeFailure` enum, the SHA-256 hasher, the upsert counter, and **both specs**. The concurrency
spec is the regression guard for the finding above and should not be dropped as "spike code".

**Throw away / rewrite before production:**

- `PerCallRepository` — one transaction per call is a spike simplification. See Finding 1.
- Spec placement — these belong in `identity-infrastructure`, which has **no test dependencies
  configured** (no h2, no spock, no `src/test`). They live in `datamanager-app` only because that
  module already has all three. Configuring `identity-infrastructure` for tests is a small, separate
  task.
- No Spring wiring, no config properties, no entity registration in the app's persistence unit, no
  UI. `expiresAt`/lifetimes are constructor parameters, not `application.properties` entries.
- `JpaAttemptCounter.increment` does upsert-then-select-then-maybe-update. Correct, but three
  round-trips; a single `RETURNING`-style statement or a `SELECT ... FOR UPDATE` wrapper would be
  tighter. Not worth optimising before it is wired.

## Deliberately not tested (out of spike scope)

- **MariaDB itself.** Everything ran on H2 in MySQL mode. The upsert and `PESSIMISTIC_WRITE` should
  be re-run against the real engine before sign-off — H2's lock semantics are not MariaDB's.
- `deleteExpiredOrConsumedBefore` (retention is undecided — #1669).
- The ORCID set-password invitation flow (#1683).
- Anything Vaadin or HTTP: no routes, no views, no mail sending.

## Recommendation

The two findings that matter are done: consumption is atomic and proven single-use, and the counter
is proven not to lose increments. Both were wrong in my first implementation, which is the argument
for having run this at all.

Before #1678 closes, one thing must be re-verified on **MariaDB**: that a `PESSIMISTIC_WRITE` lock
plus `merge` serialises two concurrent consumers the same way H2 does. That is a 30-minute check
against the throwaway instance used for the DDL verification.
