package life.qbic.identity.application.throttle

import life.qbic.identity.application.throttle.AuthAttemptThrottle.Decision
import life.qbic.identity.application.throttle.AuthAttemptThrottle.Scope
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>Origin scope for the flows that send mail (AUTH-NFR-03, #1691).</b>
 *
 * <p>These cases exist because of a reported gap: behind the real proxy, login throttling worked but
 * the password-reset form stayed open to one source rotating addresses. That was not a bug in the
 * mechanism — it was a deliberate scope decision that was <b>wrong</b>. Account scope cannot bound
 * these flows at all:</p>
 *
 * <pre>
 *   attacker sends one reset to a@x, one to b@x, one to c@x, ... from a single host
 *   -> every attempt lands in a FRESH account bucket, so no account counter ever reaches a threshold
 *   -> the application sends a mail for every one of them
 * </pre>
 *
 * <p>Per-account throttling is structurally invisible to that, which makes origin scope here the
 * primary abuse control rather than a refinement.</p>
 */
class AuthAttemptMailOriginThrottleSpec extends Specification {

  static final Instant T0 = Instant.parse('2026-11-11T10:00:00Z')

  static final ClientOrigin ATTACKER = ClientOrigin.of('203.0.113.0/24')
  static final ClientOrigin LEGITIMATE = ClientOrigin.of('198.51.100.0/24')

  /** Resend: 3 per account. Registration: 3 per account (shares the resend policy). */
  static final int ACCOUNT_LIMIT = 3
  static final int MAIL_ORIGIN_LIMIT = 5
  static final int REGISTRATION_ORIGIN_LIMIT = 8

  InMemoryCounter counter
  AuthAttemptThrottle throttled

  def setup() {
    counter = new InMemoryCounter()
    throttled = new AuthAttemptThrottle(counter,
        // login limits — unused here
        10, Duration.ofMinutes(15), Duration.ofMinutes(15),
        ACCOUNT_LIMIT, Duration.ofMinutes(15), Duration.ofMinutes(15),
        true,
        60, Duration.ofMinutes(15), Duration.ofMinutes(10),
        MAIL_ORIGIN_LIMIT, Duration.ofMinutes(15), Duration.ofMinutes(15),
        REGISTRATION_ORIGIN_LIMIT, Duration.ofMinutes(15), Duration.ofMinutes(10))
  }

  // ---------------------------------------------------------------- the reported gap, directly

  @Unroll
  def "one origin rotating addresses is throttled on #scope"() {
    // The reported defect. Every attempt uses a DIFFERENT address, so the account bucket is never
    // reused and account-scope throttling can never fire. Without an origin bucket this loop sends
    // mail indefinitely.
    given: 'an attacker that never reuses an address'
    def addresses = (1..(limit + 3)).collect { "target$it@example.org".toString() }

    when: 'each one requests a mail'
    def decisions = addresses.collect { address ->
      throttled.recordAttempt(scope, address, ATTACKER, T0)
    }

    then: 'the first `limit` are allowed and the rest are refused, though no account repeated'
    decisions.count { it == Decision.ALLOWED } == limit
    decisions.count { it == Decision.THROTTLED } == 3

    and: 'which is the whole point: the throttle saw the attacker, not just the addresses'
    counter.originCount() == limit + 3

    where:
    scope                           | limit
    Scope.PASSWORD_RESET_RESEND     | MAIL_ORIGIN_LIMIT
    Scope.EMAIL_CONFIRMATION_RESEND | MAIL_ORIGIN_LIMIT
    Scope.REGISTRATION              | REGISTRATION_ORIGIN_LIMIT
  }

  @Unroll
  def "a blocked origin is refused even for a brand-new address on #scope"() {
    given:
    (1..limit + 1).each { i ->
      throttled.recordAttempt(scope, "target$i@example.org".toString(), ATTACKER, T0)
    }

    expect: 'the pre-check refuses, so no mail is composed at all'
    throttled.isThrottled(scope, 'never-seen-before@example.org', ATTACKER, T0.plusSeconds(5))

    and: 'and the same address from a clean origin is fine — the address was never the problem'
    !throttled.isThrottled(scope, 'never-seen-before@example.org', LEGITIMATE,
        T0.plusSeconds(5))

    where:
    scope                           | limit
    Scope.PASSWORD_RESET_RESEND     | MAIL_ORIGIN_LIMIT
    Scope.EMAIL_CONFIRMATION_RESEND | MAIL_ORIGIN_LIMIT
    Scope.REGISTRATION              | REGISTRATION_ORIGIN_LIMIT
  }

  def "a mail-origin block does not spill over into login"() {
    // The buckets are independent, and they must be: blocking an abusive origin from sending mail
    // must not stop its users from logging in, which is the more disruptive failure.
    given:
    (1..MAIL_ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, "target$i@example.org".toString(),
          ATTACKER, T0)
    }

    expect:
    throttled.isThrottled(Scope.PASSWORD_RESET_RESEND, 'x@example.org', ATTACKER, T0.plusSeconds(1))
    !throttled.isThrottled(Scope.LOGIN, 'x@example.org', ATTACKER, T0.plusSeconds(1))
  }

  def "a login-origin block does not stop the same origin from requesting a reset"() {
    // The reverse direction. A user who fumbled their password ten times still needs to reset it.
    given:
    (1..61).each { i ->
      throttled.recordAttempt(Scope.LOGIN, "victim$i@example.org".toString(), ATTACKER, T0)
    }

    expect:
    throttled.isThrottled(Scope.LOGIN, 'x@example.org', ATTACKER, T0.plusSeconds(1))
    !throttled.isThrottled(Scope.PASSWORD_RESET_RESEND, 'x@example.org', ATTACKER,
        T0.plusSeconds(1))
  }

  // ---------------------------------------------------------------- the two buckets are distinct

  def "registration gets more headroom than a resend, because a semester start is a real burst"() {
    // The reason there are two buckets. Registration is legitimately bursty; a resend is not. One
    // shared bucket would either throttle a whole campus or force the resend limit high enough that
    // it stopped bounding anything.
    expect:
    REGISTRATION_ORIGIN_LIMIT > MAIL_ORIGIN_LIMIT
  }

  def "exhausting the mail bucket leaves the registration bucket intact"() {
    given:
    (1..MAIL_ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, "target$i@example.org".toString(),
          ATTACKER, T0)
    }

    expect: 'a burst of registrations from the same origin is still allowed'
    throttled.recordAttempt(Scope.REGISTRATION, 'student@example.org', ATTACKER,
        T0.plusSeconds(1)) == Decision.ALLOWED
  }

  def "exhausting the registration bucket leaves the mail bucket intact"() {
    given:
    (1..REGISTRATION_ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.REGISTRATION, "student$i@example.org".toString(), ATTACKER, T0)
    }

    expect:
    throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, 'someone@example.org', ATTACKER,
        T0.plusSeconds(1)) == Decision.ALLOWED
  }

  // ---------------------------------------------------------------- victim protection

  def "a victim whose resend bucket was filled by someone else can still request a reset"() {
    // Criterion 2 from the login work applies here too: an attacker must not be able to stop a chosen
    // user from requesting a password reset, or the DoS simply moves from mail volume to lockout.
    given: 'an attacker fills the victim address bucket from their own origin'
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, 'victim@example.org', ATTACKER,
          T0.plusSeconds(i))
    }

    expect: 'the attacker is refused for that address'
    throttled.isThrottled(Scope.PASSWORD_RESET_RESEND, 'victim@example.org', ATTACKER,
        T0.plusSeconds(20))

    and: 'but the victim, from their own network, is not'
    !throttled.isThrottled(Scope.PASSWORD_RESET_RESEND, 'victim@example.org', LEGITIMATE,
        T0.plusSeconds(20))
    throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, 'victim@example.org', LEGITIMATE,
        T0.plusSeconds(21)) == Decision.ALLOWED
  }

  // ---------------------------------------------------------------- invariants that carry over

  def "the mail origin key is a hash, never a readable network"() {
    when:
    throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, 'x@example.org', ATTACKER, T0)

    then: 'the origin bucket must not become a list of institutions under attack'
    counter.originKeys().first() ==~ /[0-9a-f]{64}/
    !counter.originKeys().first().contains('203')
  }

  def "an unknown origin writes no origin bucket and falls back to account scope"() {
    when:
    throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, 'x@example.org', ClientOrigin.unknown(), T0)

    then: 'a placeholder here would key every request on one bucket, which is the global-DoS failure'
    counter.originScopesWritten().isEmpty()
  }

  def "a live mail-origin block is not extended by retrying"() {
    given:
    (1..MAIL_ORIGIN_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, "target$i@example.org".toString(),
          ATTACKER, T0)
    }

    when: 'the blocked client keeps hammering'
    20.times { i ->
      throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, "spam$i@example.org".toString(),
          ATTACKER, T0.plusSeconds(30 + i))
    }

    then: 'the block still ends when it was going to, so a campus does not lock itself out'
    !throttled.isThrottled(Scope.PASSWORD_RESET_RESEND, 'x@example.org', ATTACKER,
        T0.plus(Duration.ofMinutes(30)))
  }

  def "a blank identifier records nothing, so it cannot be used to inflate a bucket"() {
    when:
    def decision = throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, '  ', ATTACKER, T0)

    then:
    decision == Decision.ALLOWED
    counter.totalIncrements() == 0
  }

  def "the derived attribution threshold follows the resend account limit"() {
    // Registration shares the resend account policy, so one origin filling a resend bucket must
    // implicate it at the same count the account bucket blocks on.
    given: 'an attacker fills one address bucket'
    (1..ACCOUNT_LIMIT + 1).each { i ->
      throttled.recordAttempt(Scope.PASSWORD_RESET_RESEND, 'victim@example.org', ATTACKER,
          T0.plusSeconds(i))
    }

    expect: 'the origin that did it is treated as the source, not as a bystander'
    throttled.isThrottled(Scope.PASSWORD_RESET_RESEND, 'victim@example.org', ATTACKER,
        T0.plusSeconds(30))
  }

  def "with origin scope disabled the mail flows are account-scoped as before"() {
    // The pre-#1691 behaviour, still reachable, and still the documented residual risk.
    given:
    def accountOnly = new AuthAttemptThrottle(counter, 10, Duration.ofMinutes(15),
        Duration.ofMinutes(15), ACCOUNT_LIMIT, Duration.ofMinutes(15), Duration.ofMinutes(15))

    when: 'one origin rotates addresses'
    def decisions = (1..20).collect { i ->
      accountOnly.recordAttempt(Scope.PASSWORD_RESET_RESEND, "target$i@example.org".toString(),
          ATTACKER, T0)
    }

    then: 'nothing is bounded — this is exactly the reported vulnerability'
    decisions.every { it == Decision.ALLOWED }
    counter.originScopesWritten().isEmpty()
  }

  // ---------------------------------------------------------------- test double

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

    /** Origin buckets actually written, so the flow -> bucket mapping can be asserted. */
    List<String> originScopesWritten() {
      return writtenScopes.findAll { it.contains('ORIGIN') }
    }

    Set<String> originKeys() {
      int i = 0
      def keys = [] as Set
      writtenScopes.each { scope ->
        if (scope.contains('ORIGIN')) {
          keys << writtenKeys[i]
        }
        i++
      }
      return keys
    }

    int originCount() {
      return counts.findAll { k, v -> k.contains('ORIGIN|') }.values().sum(0) { it[0] } as int
    }

    int totalIncrements() {
      return increments
    }

    private static String bucketId(String scope, String key) {
      return scope + '|' + key
    }
  }
}
