package life.qbic.identity.application.throttle

import life.qbic.identity.application.throttle.AuthAttemptThrottle.Decision
import life.qbic.identity.application.throttle.AuthAttemptThrottle.Scope
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>Server-side throttling for the unauthenticated auth flows (AUTH-NFR-03, #1682).</b>
 *
 * <p>Three properties carry the requirement, and each has its own reason:</p>
 * <ol>
 *   <li>the block is <b>server-side</b> — it lives in the counter, not in a UI-scope timer that a
 *       page reload resets;</li>
 *   <li>the throttle is <b>keyed on a hash of the address</b> and never consults the account table,
 *       so an unknown address is throttled exactly like a known one — otherwise the throttle itself
 *       answers "does this account exist?";</li>
 *   <li><b>normalisation is shared</b>, so capitalisation cannot multiply the allowance.</li>
 * </ol>
 */
class AuthAttemptThrottleSpec extends Specification {

  static final Instant T0 = Instant.parse('2026-11-11T10:00:00Z')
  static final String ADDRESS = 'alice@example.org'

  AuthAttemptCounter counter
  AuthAttemptThrottle throttle

  def setup() {
    counter = new InMemoryCounter()
    throttle = new AuthAttemptThrottle(counter,
        3, Duration.ofMinutes(15), Duration.ofMinutes(15),   // login
        2, Duration.ofMinutes(15), Duration.ofMinutes(15))   // resend
  }

  // ---- the counter is server-side ------------------------------------------------------------

  def "attempts below the threshold are allowed"() {
    expect:
    throttle.record(Scope.LOGIN, ADDRESS, T0) == Decision.ALLOWED
    throttle.record(Scope.LOGIN, ADDRESS, T0.plusSeconds(1)) == Decision.ALLOWED
    throttle.record(Scope.LOGIN, ADDRESS, T0.plusSeconds(2)) == Decision.ALLOWED
  }

  def "the attempt that crosses the threshold is itself throttled"() {
    given: 'three allowed attempts at a limit of three'
    3.times { throttle.record(Scope.LOGIN, ADDRESS, T0) }

    expect: 'the fourth is rejected, and so is every later one'
    throttle.record(Scope.LOGIN, ADDRESS, T0.plusSeconds(10)) == Decision.THROTTLED
    throttle.record(Scope.LOGIN, ADDRESS, T0.plusSeconds(11)) == Decision.THROTTLED
  }

  def "a block survives a new throttle instance, because it is in the store and not in the UI"() {
    // This is the actual defect being fixed. The old lockout was a counter in a UIScope bean, so a
    // page reload reset it to zero and the "lockout" was not a control at all.
    given:
    4.times { throttle.record(Scope.LOGIN, ADDRESS, T0) }

    when: 'a completely new service instance is built over the same store (i.e. a reload/restart)'
    def afterReload = new AuthAttemptThrottle(counter,
        3, Duration.ofMinutes(15), Duration.ofMinutes(15),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15))

    then: 'the block is still in force'
    afterReload.isThrottled(Scope.LOGIN, ADDRESS, T0.plusSeconds(30))
  }

  def "the block expires after its duration"() {
    given:
    4.times { throttle.record(Scope.LOGIN, ADDRESS, T0) }

    expect: 'blocked inside the block duration'
    throttle.isThrottled(Scope.LOGIN, ADDRESS, T0.plus(Duration.ofMinutes(14)))

    and: 'free once it has elapsed'
    !throttle.isThrottled(Scope.LOGIN, ADDRESS, T0.plus(Duration.ofMinutes(16)))
  }

  def "a successful authentication clears the counter"() {
    given: 'one attempt away from the limit'
    2.times { throttle.record(Scope.LOGIN, ADDRESS, T0) }

    when: 'the user gets it right'
    throttle.onSuccess(Scope.LOGIN, ADDRESS)

    then: 'the allowance is whole again rather than nearly exhausted'
    3.times { assert throttle.record(Scope.LOGIN, ADDRESS, T0.plusSeconds(5)) == Decision.ALLOWED }

    and:
    throttle.record(Scope.LOGIN, ADDRESS, T0.plusSeconds(6)) == Decision.THROTTLED
  }

  def "the outcome reports the counts a view needs, so it never has to guess"() {
    // A view that wants to say "2 more attempts" must not re-derive the limit or the count itself —
    // two places computing the same number is how copy and behaviour drift apart.
    when:
    def first = throttle.recordAndReport(Scope.LOGIN, ADDRESS, T0)
    def second = throttle.recordAndReport(Scope.LOGIN, ADDRESS, T0.plusSeconds(1))
    def third = throttle.recordAndReport(Scope.LOGIN, ADDRESS, T0.plusSeconds(2))
    def fourth = throttle.recordAndReport(Scope.LOGIN, ADDRESS, T0.plusSeconds(3))

    then: 'the login limit in this spec is 3'
    first.attemptsUsed() == 1
    first.attemptsRemainingInWindow() == 2
    first.isAllowed()

    and:
    third.attemptsUsed() == 3
    third.attemptsRemainingInWindow() == 0
    third.isAllowed()

    and: 'the fourth is the first refused attempt — the configured limit is honest'
    fourth.decision() == Decision.THROTTLED
    !fourth.isAllowed()
    fourth.attemptsRemainingInWindow() == 0
  }

  def "the remaining count never goes negative, even when attempts overshoot"() {
    // A client that keeps hammering pushes attemptCount past the limit. Reporting "-3 remaining" would
    // leak the internal count into copy.
    when:
    (1..8).each { throttle.recordAndReport(Scope.LOGIN, ADDRESS, T0.plusSeconds(it)) }
    def outcome = throttle.recordAndReport(Scope.LOGIN, ADDRESS, T0.plusSeconds(9))

    then:
    outcome.attemptsUsed() > outcome.maxAttempts()
    outcome.attemptsRemainingInWindow() == 0
  }

  def "record() and recordAndReport() agree on the decision"() {
    // record() delegates to recordAndReport(); this pins it so they cannot drift.
    given:
    def a = new InMemoryCounter()
    def b = new InMemoryCounter()
    def t1 = new AuthAttemptThrottle(a, 2, Duration.ofMinutes(15), Duration.ofMinutes(15),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15))
    def t2 = new AuthAttemptThrottle(b, 2, Duration.ofMinutes(15), Duration.ofMinutes(15),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15))

    expect:
    (1..4).every { i ->
      t1.record(Scope.LOGIN, ADDRESS, T0.plusSeconds(i)) ==
          t2.recordAndReport(Scope.LOGIN, ADDRESS, T0.plusSeconds(i)).decision()
    }
  }

  // ---- the two rules that cause rework if missed ---------------------------------------------

  def "the stored key is a hash, never the address"() {
    when:
    throttle.record(Scope.LOGIN, ADDRESS, T0)

    then: 'the store holds one 64-character hex digest'
    counter.keys().size() == 1
    String key = counter.keys().first()
    key ==~ /[0-9a-f]{64}/

    and: 'the raw address appears nowhere in it'
    !key.contains('alice')
    !key.contains('example')
  }

  def "the same address in different capitalisation or with padding shares one bucket"() {
    // EmailAddress performs no case folding or stripping, so without shared normalisation these
    // would be four buckets pointing at one account — and varying capitalisation would grant an
    // attacker unlimited attempts per victim.
    given:
    def variants = ['alice@example.org', 'Alice@Example.org', 'ALICE@EXAMPLE.ORG',
                    '  alice@example.org  ']

    when:
    variants.each { throttle.record(Scope.LOGIN, it, T0) }

    then: 'exactly one bucket, and it is already at the limit'
    counter.keys().size() == 1
    counter.totalIncrements() == variants.size()
  }

  def "different flows do not share a bucket"() {
    given: 'the resend limit is 2'
    3.times { throttle.record(Scope.PASSWORD_RESET_RESEND, ADDRESS, T0) }

    expect: 'a reset-resend block does not block login'
    throttle.isThrottled(Scope.PASSWORD_RESET_RESEND, ADDRESS, T0.plusSeconds(1))
    !throttle.isThrottled(Scope.LOGIN, ADDRESS, T0.plusSeconds(1))

    and: 'and not the other resend flow either'
    !throttle.isThrottled(Scope.EMAIL_CONFIRMATION_RESEND, ADDRESS, T0.plusSeconds(1))
  }

  def "the throttle has no dependency that could reveal whether an account exists"() {
    // Structural guard for Rule 1. The requirement is that a registered and an unregistered address
    // are treated identically, and the way to guarantee that is to give the throttle no way to tell
    // them apart: its only collaborators are the counter and configuration. If someone later injects
    // the user repository here "to skip throttling for real accounts", this spec fails.
    expect:
    AuthAttemptThrottle.declaredFields.findAll { !it.synthetic }.collect { it.type }.every { type ->
      !type.name.toLowerCase().contains('user')
    }
  }

  def "an unknown address is throttled exactly like a known one"() {
    given: 'the throttle has no way to tell them apart'
    def unknown = 'nobody@example.org'

    when:
    (1..5).each { throttle.record(Scope.LOGIN, ADDRESS, T0) }
    (1..5).each { throttle.record(Scope.LOGIN, unknown, T0) }

    then: 'both end up blocked'
    throttle.isThrottled(Scope.LOGIN, ADDRESS, T0.plusSeconds(1))
    throttle.isThrottled(Scope.LOGIN, unknown, T0.plusSeconds(1))

    and: 'in separate buckets, so blocking one does not help or harm the other'
    counter.keys().size() == 2
  }

  def "a victim's bucket is independent of the attacker's attempts on other addresses"() {
    // Decision B4's first rule: throttling must never let an attacker lock out a known victim by
    // spamming their address... which per-account throttling alone CANNOT prevent for that one
    // victim. This spec pins what account scope does guarantee: an attacker spamming OTHER addresses
    // cannot consume the victim's allowance.
    given:
    def victim = 'victim@example.org'

    when: 'an attacker exhausts five other addresses'
    (1..5).each { i -> throttle.record(Scope.LOGIN, "target$i@example.org", T0) }

    then: 'the victim has a full allowance'
    !throttle.isThrottled(Scope.LOGIN, victim, T0.plusSeconds(1))
    3.times { assert throttle.record(Scope.LOGIN, victim, T0.plusSeconds(2)) == Decision.ALLOWED }
  }

  @Unroll
  def "a non-positive #what is rejected at construction"() {
    when:
    new AuthAttemptThrottle(counter, maxAttempts, window, Duration.ofMinutes(1),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15))

    then:
    thrown(IllegalArgumentException)

    where:
    what          | maxAttempts | window
    'max-attempts'| 0           | Duration.ofMinutes(15)
    'negative max'| -1          | Duration.ofMinutes(15)
    'window'      | 10          | Duration.ZERO
    'neg. window' | 10          | Duration.ofMinutes(-5)
  }

  def "a blank identifier is allowed, because there is no bucket to fill"() {
    expect:
    throttle.record(Scope.LOGIN, '', T0) == Decision.ALLOWED
    throttle.record(Scope.LOGIN, '   ', T0) == Decision.ALLOWED
    throttle.record(Scope.LOGIN, null, T0) == Decision.ALLOWED
    !throttle.isThrottled(Scope.LOGIN, '', T0)
  }

  def "isThrottled does not itself count an attempt"() {
    // A rejected request is not a guess. Counting it would let a blocked client extend its own block
    // by retrying, turning a temporary throttle into a permanent lockout the attacker controls.
    when:
    10.times { throttle.isThrottled(Scope.LOGIN, ADDRESS, T0) }

    then:
    counter.totalIncrements() == 0
    !throttle.isThrottled(Scope.LOGIN, ADDRESS, T0)
  }

  // ---- test double ---------------------------------------------------------------------------

  /** Faithful enough to exercise the service: one bucket per (scope, key), blocking is sticky. */
  static final class InMemoryCounter implements AuthAttemptCounter {

    private final Map<String, int[]> counts = [:]
    private final Map<String, Instant> blocks = [:]
    private int increments = 0

    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      increments++
      String id = "$scope\u0000$key"
      counts.computeIfAbsent(id) { new int[1] }[0]++
      return new Counter(counts[id][0], blocks[id])
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
      blocks["$scope\u0000$key"] = blockedUntil
    }

    @Override
    Counter peek(String scope, String key) {
      String id = "$scope\u0000$key"
      new Counter(counts[id] == null ? 0 : counts[id][0], blocks[id])
    }

    @Override
    void clear(String scope, String key) {
      String id = "$scope\u0000$key"
      counts.remove(id)
      blocks.remove(id)
    }

    Set<String> keys() {
      return counts.keySet().collect { it.split('\u0000')[1] } as Set
    }

    int totalIncrements() {
      return increments
    }
  }
}
