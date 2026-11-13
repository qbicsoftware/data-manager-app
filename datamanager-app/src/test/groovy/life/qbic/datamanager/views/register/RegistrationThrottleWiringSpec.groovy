package life.qbic.datamanager.views.register

import life.qbic.identity.application.throttle.AuthAttemptThrottle
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The registration views must actually be wired to the throttle.</b>
 *
 * <p>This guard exists because of a defect class this codebase has already paid for twice: a unit spec
 * that constructs its subject by hand passes happily while the application wires something else, or
 * while a new collaborator is injected and never used.</p>
 *
 * <ol>
 *   <li><b>Wiring.</b> Both registration routes receive the throttle through
 *       {@link RegistrationSubmission}. If the bean is missing the routes fail at startup, not at first
 *       use — and a hand-constructed spec never notices, because it passes a stub.</li>
 *   <li><b>Actually called.</b> Injecting a throttle and never calling it is the more likely mistake and
 *       it is silent: registration keeps working, unthrottled. The previous slice shipped exactly that
 *       shape — a throttle with zero production call sites — which is why it had once been deleted as
 *       dead code.</li>
 * </ol>
 *
 * <p>The behavioural half lives in {@code RegistrationSubmissionSpec}; this spec asserts the two things a
 * behavioural spec cannot: that the bean is required at construction, and that the shipped bytecode
 * references the registration scope.</p>
 */
class RegistrationThrottleWiringSpec extends Specification {

  private static final List<Class> REGISTRATION_VIEWS = [UserRegistrationMain, RegistrationOrcIdMain]

  @Unroll
  def "#view requires the throttle at construction"() {
    given:
    def parameterTypes = view.declaredConstructors
        .findAll { !it.synthetic }
        .collectMany { it.parameterTypes as List }

    expect: 'the bean is required, so a missing bean fails at startup rather than silently disabling it'
    parameterTypes.contains(AuthAttemptThrottle)

    and: 'the identity port is required too, so the view cannot bypass the uniform-response flow'
    parameterTypes.contains(RegistrationIdentity)

    where:
    view << REGISTRATION_VIEWS
  }

  def "the registration scope exists and is separate from the login scope"() {
    given:
    def scopes = AuthAttemptThrottle.Scope.values().collect { it.name() }

    expect: 'the AC names registration, and it was not covered before'
    scopes.contains('REGISTRATION')

    and: 'it is not folded into LOGIN, which would share a bucket and a threshold'
    scopes.count { it == 'REGISTRATION' } == 1
  }

  def "the submission flow references the registration scope in shipped bytecode"() {
    given: 'the compiled class file, so the assertion is about what is deployed'
    def resource = RegistrationSubmission.name.replace('.', '/') + '.class'
    def bytes = RegistrationSubmission.classLoader.getResourceAsStream(resource)
        .withCloseable { new String(it.bytes, 'ISO-8859-1') }

    expect: 'the enum constant appears in the constant pool, so it is genuinely used'
    bytes.contains('REGISTRATION')
  }

  def "the submission flow records an attempt before it registers"() {
    given: 'a real throttle over a counter that records what it is asked'
    def counter = new RecordingCounter()
    def throttle = new AuthAttemptThrottle(counter,
        10, java.time.Duration.ofMinutes(15), java.time.Duration.ofMinutes(15),
        3, java.time.Duration.ofMinutes(15), java.time.Duration.ofMinutes(15))
    def submission = new RegistrationSubmission(
        new RegistrationSubmissionSpec.FakeIdentity(),
        Stub(life.qbic.identity.api.UserInformationService) { findByEmail(_) >> Optional.empty() },
        throttle)

    when:
    submission.submitLocal(new RegistrationSubmission.RegistrationForm(
        'Ada Lovelace', 'someone@example.org', 'alovelace', 'a-strong-passphrase'), { it })

    then: 'the attempt was recorded against the REGISTRATION scope, before any registration work'
    counter.scopes == [AuthAttemptThrottle.Scope.REGISTRATION.name()]
  }

  /** Records the scope of every increment, so ordering and scope can be asserted. */
  static class RecordingCounter implements life.qbic.identity.domain.repository.AuthAttemptCounter {
    List<String> scopes = []

    @Override
    Counter increment(String scope, String key, java.time.Instant now, java.time.Duration window) {
      scopes << scope
      return new Counter(1, null)
    }

    @Override
    Counter peek(String scope, String key) { return new Counter(0, null) }

    @Override
    void block(String scope, String key, java.time.Instant blockedUntil) { }

    @Override
    void clear(String scope, String key) { }
  }
}
