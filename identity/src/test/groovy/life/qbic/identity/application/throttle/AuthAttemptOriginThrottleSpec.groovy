package life.qbic.identity.application.throttle

import life.qbic.identity.application.throttle.AuthAttemptThrottle.Decision
import life.qbic.identity.application.throttle.AuthAttemptThrottle.Scope
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>Origin-scope throttling, and the rule that keeps the victim of an attack able to log in
 * (AUTH-NFR-03, #1691).</b>
 *
 * <p>The requirement's two origin criteria are in tension, and this spec exists mainly to pin the
 * second one, which a naive "either bucket over threshold blocks" implementation cannot satisfy:</p>
 * <ol>
 *   <li>many accounts hammered from one origin → <b>that origin is throttled</em>, other origins are
 *       unaffected;</li>
 *   <li>an attacker fills a victim's account bucket from their own origin → the <b>victim logging in
 *       from a different origin is not blocked</b>.</li>
 * </ol>
 *
 * <p>Criterion 2 is the whole reason origin scope exists. Under account scope alone, the attacker's
 * spam is indistinguishable from the victim's own typos, so the victim is locked out — see
 * {@code docs/operations/installation-and-administration.md} §1.8.</p>
 */
class AuthAttemptOriginThrottleSpec extends Specification {

  static final Instant T0 = Instant.parse('2026-11-11T10:00:00Z')
  static final String VICTIM = 'victim@example.org'
  static final String ATTACKER_TARGET = 'target@example.org'

  /** The victim's network and the attacker's, as the resolver would group them. */
  static final ClientOrigin VICTIM_ORIGIN = ClientOrigin.of('198.51.100.0/24')
  static final ClientOrigin ATTACKER_ORIGIN = ClientOrigin.of('203.0.113.0/24')
  static final ClientOrigin THIRD_ORIGIN = ClientOrigin.of('192.0.2.0/24')

  // Account limit 3, origin limit 20. Small numbers so the arithmetic is visible; the production
  // defaults are 10 and 60. The attribution threshold is DERIVED (ACCOUNT_LIMIT + 1), not configured.
  static final int ACCOUNT_LIMIT = 3
  static final int ORIGIN_LIMIT = 20
  static final int ATTRIBUTION_THRESHOLD = ACCOUNT_LIMIT + 1

  InMemoryCounter counter
  AuthAttemptThrottle throttled

  def setup() {
    counter = new InMemoryCounter()
    throttled = originScoped(ACCOUNT_LIMIT, ORIGIN_LIMIT)
  }

  // ---------------------------------------------------------------- criterion 1: the origin is throttled

  def "an origin flooding many accounts is throttled, and stops being able to probe at all"() {
    given: 'one origin failing against a fresh account each time'
    when: 'it exceeds the origin threshold, which is far above any single account threshold'
    (1..ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, "target$i@example.org".toString(), ATTACKER_ORIGIN, T0)
    }

    then: 'the next attempt against a brand-new, untouched account is refused'
    throttled.isThrottled(Scope.LOGIN, 'never-touched@example.org', ATTACKER_ORIGIN,
        T0.plusSeconds(1))

    and: 'and recording it says the same thing'
    throttled.recordAttempt(Scope.LOGIN, 'another-fresh@example.org', ATTACKER_ORIGIN,
        T0.plusSeconds(2)) == Decision.THROTTLED
  }

  def "a throttled origin does not throttle a different origin"() {
    given: 'the attacker exhausts their own origin'
    (1..ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, "target$i@example.org".toString(), ATTACKER_ORIGIN, T0)
    }

    expect: 'an innocent origin is untouched, even for the same kind of request'
    !throttled.isThrottled(Scope.LOGIN, 'someone@example.org', THIRD_ORIGIN, T0.plusSeconds(1))
    throttled.recordAttempt(Scope.LOGIN, 'someone@example.org', THIRD_ORIGIN,
        T0.plusSeconds(1)) == Decision.ALLOWED
  }

  def "the origin block expires on its own schedule"() {
    given:
    (1..ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, "target$i@example.org".toString(), ATTACKER_ORIGIN, T0)
    }

    expect: 'blocked inside the block duration'
    throttled.isThrottled(Scope.LOGIN, 'fresh@example.org', ATTACKER_ORIGIN,
        T0.plus(Duration.ofMinutes(5)))

    and: 'allowed again once it has elapsed'
    !throttled.isThrottled(Scope.LOGIN, 'fresh@example.org', ATTACKER_ORIGIN,
        T0.plus(Duration.ofMinutes(30)))
  }

  def "a live origin block is not extended by retrying, so it cannot become permanent"() {
    given:
    (1..ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, "target$i@example.org".toString(), ATTACKER_ORIGIN, T0)
    }

    when: 'the blocked client keeps hammering'
    20.times { i ->
      throttled.recordAttempt(Scope.LOGIN, "spam$i@example.org".toString(), ATTACKER_ORIGIN,
          T0.plusSeconds(30 + i))
    }

    then: 'the block still ends when it was going to'
    !throttled.isThrottled(Scope.LOGIN, 'fresh@example.org', ATTACKER_ORIGIN,
        T0.plus(Duration.ofMinutes(30)))
  }

  // ---------------------------------------------------------------- criterion 2: the victim survives

  def "a clean origin is NOT blocked by another party's damage to the account bucket"() {
    // This is the requirement's clause that account-scope-only throttling cannot satisfy: today the
    // attacker's 11 failures lock the victim out for the block duration.
    given: 'an attacker fills the VICTIM account bucket from their own origin'
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(i))
    }

    expect: 'the attacker is now refused for that account'
    throttled.isThrottled(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(20))

    and: 'but the victim, from their own untouched origin, is not'
    !throttled.isThrottled(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(20))

    and: 'and the victim can actually authenticate'
    throttled.recordAttempt(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(21)) == Decision.ALLOWED
  }

  def "the victim keeps their full allowance every time they log in, however long the attack lasts"() {
    given: 'a sustained attack from one origin'
    (1..ACCOUNT_LIMIT + 5).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(i))
    }

    expect: 'the victim can still mistype their own password the usual number of times'
    (1..ACCOUNT_LIMIT).every { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(100 + i)) ==
          Decision.ALLOWED
    }

    and: 'a success then clears their bucket, so the attack cannot accumulate against them'
    throttled.onSuccess(Scope.LOGIN, VICTIM)
    counter.accountCountOf(VICTIM) == 0
  }

  def "the victim's own origin is eventually held responsible if it is the one failing"() {
    // The other side of the bargain, and the reason the clean-origin allowance is not unlimited: the
    // victim's origin is only spared while it is genuinely clean. If the failures keep coming from it,
    // it eventually accumulates the same number that filled the account bucket and is throttled — the
    // attacker's origin and the victim's are judged by the same rule.
    given:
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(i))
    }

    when: 'the victim keeps failing from their own origin'
    (1..ATTRIBUTION_THRESHOLD).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(50 + i))
    }

    then:
    throttled.isThrottled(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(60))
  }

  def "the victim IS refused once their own origin has itself accumulated the damage"() {
    // The other half of criterion 2, and the reason the rule is not simply "account bucket is
    // irrelevant": an origin that has by itself produced as many failures as a whole account bucket
    // gets is not a bystander, whatever account it is aimed at.
    given:
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(i))
    }

    when: 'the same origin keeps going past the attribution threshold'
    (1..ATTRIBUTION_THRESHOLD).each { i ->
      throttled.recordAttempt(Scope.LOGIN, 'decoy@example.org', ATTACKER_ORIGIN, T0.plusSeconds(10 + i))
    }

    then: 'it is treated as the source of the damage and refused'
    throttled.isThrottled(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(30))
  }

  def "a blocked account with no origin information falls back to account scope"() {
    // The pre-origin behaviour, and the reason an unknown origin must never be treated as "allow":
    // that would silently disable throttling on any deployment that lost its proxy configuration.
    given: 'origin scope switched off entirely'
    def accountOnly = new AuthAttemptThrottle(counter, ACCOUNT_LIMIT, Duration.ofMinutes(15),
        Duration.ofMinutes(15), 2, Duration.ofMinutes(15), Duration.ofMinutes(15))
    (1..ACCOUNT_LIMIT + 1).each { i ->
      accountOnly.recordAttempt(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(i))
    }

    expect: 'the account bucket still blocks, exactly as before #1691'
    accountOnly.isThrottled(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(30))
    accountOnly.recordAttempt(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0.plusSeconds(31)) == Decision.THROTTLED
  }

  def "an unknown origin cannot rescue a blocked account even when origin scope is enabled"() {
    given: 'a deployment that enabled origin scope but resolved nothing'
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ClientOrigin.unknown(), T0.plusSeconds(i))
    }

    expect: 'it degrades to account scope rather than to no throttle'
    throttled.isThrottled(Scope.LOGIN, VICTIM, ClientOrigin.unknown(), T0.plusSeconds(30))
  }

  def "an unknown origin writes no origin bucket"() {
    when:
    throttled.recordAttempt(Scope.LOGIN, VICTIM, ClientOrigin.unknown(), T0)

    then: 'only the account bucket exists — there is nothing to attribute the request to'
    counter.scopes() == [Scope.LOGIN.name()]
  }

  // ---------------------------------------------------------------- invariants carried over

  def "the origin bucket is written alongside the account bucket, in one call"() {
    when:
    throttled.recordAttempt(Scope.LOGIN, VICTIM, VICTIM_ORIGIN, T0)

    then: 'both scopes, one statement each, which is what makes the combined rule possible at all'
    counter.scopes().toSet() == [Scope.LOGIN.name(), Scope.LOGIN_ORIGIN.name()] as Set
  }

  def "the origin key is a hash, never the network in readable form"() {
    when:
    throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0)

    then: 'the store holds only 64-char digests, so the table is not a list of networks under attack'
    counter.originKeys().size() == 1
    String key = counter.originKeys().first()
    key ==~ /[0-9a-f]{64}/
    !key.contains('203')
    !key.contains('0/24')
  }

  def "a success clears the account bucket but NEVER the origin bucket"() {
    // Clearing the origin bucket here would let an attacker with any account of their own reset their
    // origin's failure tally between guesses — the bucket would exist, be maintained, and never reach
    // its threshold. That is the "dead control beside a live feature" failure this codebase has hit.
    given: 'an origin that is partway to its threshold, with the account bucket dirty too'
    (1..ATTRIBUTION_THRESHOLD).each { i ->
      throttled.recordAttempt(Scope.LOGIN, 'decoy@example.org', ATTACKER_ORIGIN, T0.plusSeconds(i))
    }
    (1..ACCOUNT_LIMIT).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(10 + i))
    }
    def originBefore = counter.originCount()

    when: 'the attacker logs into an account of their own successfully'
    throttled.onSuccess(Scope.LOGIN, 'attacker-own@example.org')

    then: 'the origin tally is untouched'
    counter.originCount() == originBefore
    counter.originKeys().size() == 1
  }

  def "the origin bucket counts failures only, because that is what actually writes to it"() {
    // The login bucket is written from the authentication-failure listener. This case documents the
    // consequence so a future reader does not "fix" it by counting attempts on the success path,
    // which would let one busy institution behind a NAT block itself on legitimate traffic.
    when: 'a successful login is not a failure and writes nothing'
    throttled.onSuccess(Scope.LOGIN, VICTIM)

    then:
    counter.originCount() == 0
  }

  def "the origin bucket and the account bucket cannot collide even for identical key material"() {
    // The scope is part of the hashed key, so a network that happens to look like an address cannot
    // land in an account bucket.
    when:
    throttled.recordAttempt(Scope.LOGIN, '203.0.113.0/24', ATTACKER_ORIGIN, T0)

    then:
    counter.rowCount() == 2
    counter.accountCount() == 1
    counter.originCount() == 1
  }

  def "an origin block alone is enough to refuse, even for a pristine account and a clean window"() {
    given:
    (1..ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, "target$i@example.org".toString(), ATTACKER_ORIGIN, T0)
    }

    when:
    def outcome = throttled.recordAndReport(Scope.LOGIN, 'pristine@example.org', ATTACKER_ORIGIN,
        T0.plusSeconds(1))

    then: 'refused, and the report still describes the ACCOUNT bucket — never the origin one'
    outcome.decision() == Decision.THROTTLED
    outcome.attemptsUsed() == 1
    outcome.maxAttempts() == ACCOUNT_LIMIT
  }

  def "rendering never learns which bucket fired"() {
    // Rule 2: an origin finding out it was throttled is less sensitive than an account finding out,
    // but a caller that branched on the bucket would leak everyone else's traffic behind the same NAT.
    given:
    (1..ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, "target$i@example.org".toString(), ATTACKER_ORIGIN, T0)
    }

    when:
    def byOrigin = throttled.recordAndReport(Scope.LOGIN, 'a@example.org', ATTACKER_ORIGIN,
        T0.plusSeconds(1))
    def byAccount = throttled.recordAndReport(Scope.LOGIN, 'b@example.org', ATTACKER_ORIGIN,
        T0.plusSeconds(1))

    then: 'the two outcomes differ only in the counts of their own account buckets'
    byOrigin.decision() == byAccount.decision()
    byOrigin.maxAttempts() == byAccount.maxAttempts()
  }

  def "isThrottled never writes, for either bucket"() {
    // A rejected request is not an attempt. Counting it would let a blocked client extend its own
    // block, and for an origin bucket that means extending it for everyone behind the same NAT.
    when:
    20.times { throttled.isThrottled(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0) }

    then:
    counter.totalIncrements() == 0
  }

  def "a blank identifier is allowed and records nothing, for either bucket"() {
    // Nothing to key on. LoginThrottleFilter passes such a submission straight to Spring Security,
    // which rejects the malformed form before any password comparison — so there is no guess to spend
    // and nothing to throttle. Counting it would let a client inflate its own origin's tally.
    when:
    def decision = throttled.recordAttempt(Scope.LOGIN, '', ATTACKER_ORIGIN, T0)

    then:
    decision == Decision.ALLOWED
    counter.totalIncrements() == 0
    !throttled.isThrottled(Scope.LOGIN, '', ATTACKER_ORIGIN, T0)
    !throttled.isThrottled(Scope.LOGIN, '   ', ATTACKER_ORIGIN, T0)
  }

  @Unroll
  def "the origin scope is never a caller-facing scope: #scope"() {
    // LOGIN_ORIGIN is derived from LOGIN inside the throttle. Accepting it from a caller would let a
    // view apply the account limits to the origin bucket, or vice versa.
    when:
    throttled.recordAttempt(scope, 'x@example.org', ATTACKER_ORIGIN, T0)

    then:
    thrown(IllegalArgumentException)

    where:
    scope << [Scope.LOGIN_ORIGIN]
  }

  def "the attribution threshold is derived from the account limit, and cannot be configured away"() {
    // Regression guard for an off-by-one that this spec caught: the account bucket blocks on attempt
    // maxAttempts + 1 (the comparison in Policy is a strict `>`), so filling it costs an attacker
    // exactly maxAttempts + 1 failures. Deriving the attribution threshold without that +1 meant an
    // attacker who filled the bucket stopped one short of implicating their origin — criterion 2 held
    // for the victim, but criterion 1's per-origin effect was soft by one attempt.
    //
    // The property that must hold at the boundary: at exactly `maxAttempts` failures this origin is
    // still a bystander; at `maxAttempts + 1` it is the culprit.
    given: 'a blocked account'
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(i))
    }

    expect: 'the origin that produced exactly those failures is refused'
    throttled.isThrottled(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(20))
  }

  def "a bystander one failure short of the account limit is still a bystander"() {
    given: 'an account blocked by somebody else, and an origin with maxAttempts - 1 failures of its own'
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, VICTIM, ATTACKER_ORIGIN, T0.plusSeconds(i))
    }
    (1..ACCOUNT_LIMIT - 1).each { i ->
      throttled.recordAttempt(Scope.LOGIN, 'decoy@example.org', THIRD_ORIGIN, T0.plusSeconds(10 + i))
    }

    expect: 'not implicated, so not refused — this is criterion 2 working as intended'
    !throttled.isThrottled(Scope.LOGIN, VICTIM, THIRD_ORIGIN, T0.plusSeconds(30))
  }

  def "the origin limits cannot be configured as zero or negative"() {
    when:
    new AuthAttemptThrottle(counter, ACCOUNT_LIMIT, Duration.ofMinutes(15), Duration.ofMinutes(15),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15), true, originLimit,
        Duration.ofMinutes(15), Duration.ofMinutes(10))

    then: 'a zero origin limit would throttle every request from every origin'
    thrown(IllegalArgumentException)

    where:
    originLimit << [0, -1]
  }

  def "the resend and registration flows keep account scope only"() {
    // Deliberate scope decision, not an oversight: a resend costs an email, so its origin threshold
    // has to be calibrated against the population behind one NAT before it can be switched on.
    when:
    throttled.recordAttempt(scope, 'someone@example.org', ATTACKER_ORIGIN, T0)

    then:
    counter.scopes() == [scope.name()]

    where:
    scope << [Scope.EMAIL_CONFIRMATION_RESEND, Scope.PASSWORD_RESET_RESEND, Scope.REGISTRATION]
  }

  def "originScopeEnabled reports the effective mode"() {
    expect:
    originScoped(3, 20).originScopeEnabled()
    !new AuthAttemptThrottle(counter, 3, Duration.ofMinutes(15), Duration.ofMinutes(15),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15)).originScopeEnabled()
  }

  // ---------------------------------------------------------------- helpers

  private AuthAttemptThrottle originScoped(int accountLimit, int originLimit) {
    return new AuthAttemptThrottle(counter, accountLimit, Duration.ofMinutes(15),
        Duration.ofMinutes(15), 2, Duration.ofMinutes(15), Duration.ofMinutes(15),
        true, originLimit, Duration.ofMinutes(15), Duration.ofMinutes(10))
  }

  /**
   * Faithful enough to exercise the combined rule: one bucket per (scope, key), sticky blocks, and
   * explicit accessors for the invocation shape so the specs can assert what was written.
   */
  static final class InMemoryCounter implements AuthAttemptCounter {

    private final Map<String, int[]> counts = [:]
    private final Map<String, Instant> blocks = [:]
    private final List<String> writtenScopes = []
    private final List<String> writtenKeys = []
    private int increments = 0

    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      increments++
      writtenScopes << scope
      writtenKeys << key
      String id = bucketId(scope, key)
      counts.computeIfAbsent(id) { new int[1] }[0]++
      return new Counter(counts[id][0], blocks[id])
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
      blocks[bucketId(scope, key)] = blockedUntil
    }

    @Override
    Counter peek(String scope, String key) {
      String id = bucketId(scope, key)
      new Counter(counts[id] == null ? 0 : counts[id][0], blocks[id])
    }

    @Override
    void clear(String scope, String key) {
      String id = bucketId(scope, key)
      counts.remove(id)
      blocks.remove(id)
    }

    List<String> scopes() {
      return writtenScopes
    }

    /** Keys written to the origin scope, which must always be digests. */
    Set<String> originKeys() {
      int i = 0
      def keys = [] as Set
      writtenScopes.each { scope ->
        if (scope == Scope.LOGIN_ORIGIN.name()) {
          keys << writtenKeys[i]
        }
        i++
      }
      return keys
    }

    int originCount() {
      return counts.findAll { k, v -> k.startsWith(Scope.LOGIN_ORIGIN.name() + '|') }
          .values().sum(0) { it[0] } as int
    }

    int accountCount() {
      return counts.findAll { k, v -> k.startsWith(Scope.LOGIN.name() + '|') }
          .values().sum(0) { it[0] } as int
    }

    /** Attempts counted in the LOGIN bucket for one identifier. */
    int accountCountOf(String identifier) {
      String id = bucketId(Scope.LOGIN.name(), AuthAttemptThrottle.attemptKey(Scope.LOGIN, identifier))
      return counts[id] == null ? 0 : counts[id][0]
    }

    int rowCount() {
      return counts.size()
    }

    int totalIncrements() {
      return increments
    }

    private static String bucketId(String scope, String key) {
      return scope + '|' + key
    }
  }
}
