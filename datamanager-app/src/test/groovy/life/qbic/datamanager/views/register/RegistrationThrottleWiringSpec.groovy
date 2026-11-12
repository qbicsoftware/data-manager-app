package life.qbic.datamanager.views.register

import life.qbic.identity.api.UserInformationService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.user.IdentityService
import life.qbic.identity.domain.repository.AuthAttemptCounter

import java.time.Duration
import java.time.Instant
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The registration views must receive the throttle, and must actually consult it.</b>
 *
 * <p>Both guards here exist because of a defect class this codebase has already paid for twice: a unit
 * spec that constructs its subject by hand will happily pass while the application wires something
 * else, or while the new collaborator is injected and never used.</p>
 *
 * <ol>
 *   <li><b>Wiring.</b> {@code AuthAttemptThrottle} was added as a constructor dependency of both
 *       registration views. If the bean is not resolvable the routes fail at startup, not at first use.
 *       A hand-constructed spec never notices, because it passes a stub.</li>
 *   <li><b>Actually called.</b> Injecting a throttle and forgetting to call it is the more likely
 *       mistake, and it is silent: registration keeps working, unthrottled. The previous slice shipped
 *       exactly that shape of bug — a throttle with zero production call sites — which is why it had
 *       been deleted as dead code once before.</li>
 * </ol>
 *
 * <p>There is no {@code @SpringBootTest} in this module (a full context needs MariaDB, Artemis, SMTP,
 * OpenBIS and the vault), so the properties are asserted directly rather than by booting.</p>
 */
class RegistrationThrottleWiringSpec extends Specification {

  private static final List<Class> REGISTRATION_VIEWS = [UserRegistrationMain, RegistrationOrcIdMain]

  @Unroll
  def "#view takes the throttle as a constructor dependency"() {
    given:
    def parameterTypes = view.declaredConstructors
        .findAll { !it.synthetic }
        .collectMany { it.parameterTypes as List }

    expect: 'the bean is required by the constructor, so a missing bean fails at startup'
    parameterTypes.contains(AuthAttemptThrottle)

    and: 'the pre-existing dependencies are still required, so this did not replace anything'
    parameterTypes.contains(IdentityService)
    parameterTypes.contains(UserInformationService)

    where:
    view << REGISTRATION_VIEWS
  }

  @Unroll
  def "#view records a REGISTRATION-scoped attempt before registering"() {
    given: 'the scope exists and is served by the resend policy'
    def scopes = AuthAttemptThrottle.Scope.values().collect { it.name() }

    expect: 'the registration scope was added — the AC names registration, and it was not covered before'
    scopes.contains('REGISTRATION')

    and: 'the view references it, so the throttle is not merely injected and ignored'
    // The enum constant appears in the class file as a constant-pool entry (a getstatic field name);
    // the textual form "Scope.REGISTRATION" never appears in bytecode, so match the constant alone.
    constantsOf(view).contains('REGISTRATION')

    where:
    view << REGISTRATION_VIEWS
  }

  def "the registration scope is throttled, not silently mapped to the login policy"() {
    given:
    def throttle = new AuthAttemptThrottle(
        new InMemoryAuthAttemptCounter(),
        10, Duration.ofMinutes(15), Duration.ofMinutes(15),
        3, Duration.ofMinutes(15), Duration.ofMinutes(15))

    when: 'a visitor submits registration more often than the resend allowance'
    def decisions = (1..4).collect {
      throttle.record(AuthAttemptThrottle.Scope.REGISTRATION, 'someone@example.org',
          Instant.parse('2026-11-12T10:00:00Z'))
    }

    then: 'the resend allowance applies, so one address cannot mass-submit'
    decisions.count { it == AuthAttemptThrottle.Decision.THROTTLED } == 1
  }

  /**
   * The compiled class file, as a string, so the assertion is about shipped bytecode rather than about
   * a test-only copy of the logic. The constant pool contains the enum constant name.
   */
  private static String constantsOf(Class type) {
    def resource = type.name.replace('.', '/') + '.class'
    def stream = type.classLoader.getResourceAsStream(resource)
    assert stream != null: "could not load bytecode for ${type.name}"
    return stream.withCloseable { new String(it.bytes, 'ISO-8859-1') }
  }

  /**
   * A counter with no database, so the policy can be exercised without MariaDB.
   *
   * <p>Mirrors the SQL contract deliberately: the window rolls over when it has elapsed and the count
   * restarts at 1, not 0. A sliding interpretation leaves the counter stuck — the exact bug that made
   * the throttle never engage in production while every spec passed on a constant clock.</p>
   */
  static class InMemoryAuthAttemptCounter implements AuthAttemptCounter {

    private final Map<String, State> states = [:]

    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      def id = "${scope}|${key}"
      def existing = states[id]
      def start = existing?.windowStart
      def count = existing?.attemptCount ?: 0
      def blocked = existing?.blockedUntil
      if (start == null || !now.isBefore(start.plus(window))) {
        start = now
        count = 0
        blocked = null
      }
      def updated = new State(attemptCount: count + 1, windowStart: start, blockedUntil: blocked)
      states[id] = updated
      return new Counter(updated.attemptCount, updated.blockedUntil)
    }

    @Override
    Counter peek(String scope, String key) {
      def existing = states["${scope}|${key}"]
      return existing == null
          ? new Counter(0, null)
          : new Counter(existing.attemptCount, existing.blockedUntil)
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
      def id = "${scope}|${key}"
      def existing = states[id]
      if (existing != null) {
        existing.blockedUntil = blockedUntil
      }
    }

    @Override
    void clear(String scope, String key) {
      states.remove("${scope}|${key}")
    }

    Set<String> keys() {
      return states.keySet()
    }

    static class State {
      int attemptCount
      Instant windowStart
      Instant blockedUntil
    }
  }
}
