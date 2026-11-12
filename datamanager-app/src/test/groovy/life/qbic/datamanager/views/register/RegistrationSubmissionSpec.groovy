package life.qbic.datamanager.views.register

import life.qbic.application.commons.ApplicationResponse
import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException
import life.qbic.identity.application.user.IdentityService.UserExistsException
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>A duplicate registration must be indistinguishable from a successful one (AUTH-NFR-03,
 * FEAT-AUTH-FLOWS-03).</b>
 *
 * <p>These are the behaviours nothing could assert before: the logic lived in a private method on a
 * {@code @UIScope} view, welded to {@code getUI().navigate(...)} and a {@code final} service. No spec
 * could observe that a duplicate registration notified the owner, or that a throttled submission
 * disclosed nothing.</p>
 *
 * <p>Three properties carry the requirement:</p>
 * <ol>
 *   <li>a duplicate navigates exactly where a success navigates, and renders no failures;</li>
 *   <li>the owner is notified, so the outcome is not a silent dead end;</li>
 *   <li>a throttled submission behaves like a success rather than revealing the throttle — the bucket is
 *       keyed on the submitted address, so admitting it would restore the oracle.</li>
 * </ol>
 */
class RegistrationSubmissionSpec extends Specification {

  static final String EXISTING = 'existing@example.org'
  static final String NEW = 'new@example.org'
  static final String PASSWORD = 'a-strong-passphrase'

  FakeIdentity identity
  UserInformationService userInformationService
  List<String> navigations


  def setup() {
    identity = new FakeIdentity()
    userInformationService = Stub(UserInformationService) {
      findByEmail(EXISTING) >> Optional.of(
          new UserInfo('user-1', 'Existing Person', EXISTING, 'existing', true, null, null))
      findByEmail(NEW) >> Optional.empty()
    }
    navigations = []
  }

  private RegistrationSubmission submission(boolean blocked) {
    def throttle = new AuthAttemptThrottle(
        blocked ? new BlockedCounter() : new AllowingCounter(),
        10, Duration.ofMinutes(15), Duration.ofMinutes(15),
        3, Duration.ofMinutes(15), Duration.ofMinutes(15))
    return new RegistrationSubmission(identity, userInformationService, throttle)
  }

  private RegistrationSubmission.Navigator navigator() {
    return { Optional<String> id ->
      navigations << (id.orElse('<none>'))
    } as RegistrationSubmission.Navigator
  }

  private static RegistrationSubmission.RegistrationForm form(String email) {
    return new RegistrationSubmission.RegistrationForm('Ada Lovelace', email, 'alovelace', PASSWORD)
  }

  // ---- a duplicate is a registration ------------------------------------------------------------

  def "a duplicate registration renders no failure and navigates where a success navigates"() {
    given: 'an address that already has an account'
    def subject = submission(false)
    identity.localResponse = ApplicationResponse.failureResponse(new UserExistsException())

    when:
    def failures = subject.submitLocal(form(EXISTING), navigator())

    then: 'nothing is rendered — "already in use" is the oracle'
    failures.isEmpty()

    and: 'the owner is notified, so this is not a silent dead end'
    identity.resetRequests == [EXISTING]

    and: 'the visitor lands on the pending-confirmation page, like a real registration'
    navigations == ['user-1']
  }

  def "a successful registration notifies nobody"() {
    given:
    def subject = submission(false)
    identity.localResponse = ApplicationResponse.successResponse()

    when:
    def failures = subject.submitLocal(form(NEW), navigator())

    then: 'no mail the visitor did not ask for'
    failures.isEmpty()
    identity.resetRequests.isEmpty()
    navigations == ['<none>']
  }

  // ---- a throttled submission is a normal submission ---------------------------------------------

  @Unroll
  def "a throttled #flow submission is reported like a success"() {
    given:
    def subject = submission(true)

    when:
    def failures = flow == 'local'
        ? subject.submitLocal(form(NEW), navigator())
        : subject.submitOidc(form(NEW), 'issuer', 'subject', false, navigator())

    then: 'no registration is attempted, because the throttle runs first'
    identity.localRegistrations.isEmpty()
    identity.openIdRegistrations.isEmpty()

    and: 'nothing reveals that the throttle engaged'
    failures.isEmpty()
    identity.resetRequests.isEmpty()

    and: 'the visitor still reaches the pending-confirmation page'
    navigations == ['<none>']

    where:
    flow << ['local', 'ORCID']
  }

  // ---- a reportable failure is still reported ----------------------------------------------------

  @Unroll
  def "a #reason failure is still reported"() {
    given:
    def subject = submission(false)
    identity.localResponse = ApplicationResponse.failureResponse(failure)

    when:
    def failures = subject.submitLocal(form(NEW), navigator())

    then: 'the visitor is told, and not navigated'
    failures.size() == 1
    navigations.isEmpty()

    and: 'and no notification is sent, because no duplicate was involved'
    identity.resetRequests.isEmpty()

    where:
    reason           | failure
    'taken username' | new UserNameNotAvailableException()
    'empty username' | new EmptyUserNameException()
  }

  def "an ORCID assertion that is already linked registers nothing"() {
    given:
    def subject = submission(false)

    when:
    def failures = subject.submitOidc(form(EXISTING), 'issuer', 'subject', true, navigator())

    then:
    failures.isEmpty()
    identity.openIdRegistrations.isEmpty()
    identity.resetRequests.isEmpty()
  }

  /**
   * A hand-written fake, because Spock cannot mock the final IdentityService and a Mock of the narrow
   * port still made array-argument stubbing fragile. Explicit is better here.
   */
  static class FakeIdentity implements RegistrationIdentity {

    ApplicationResponse localResponse = ApplicationResponse.successResponse()
    ApplicationResponse openIdResponse = ApplicationResponse.successResponse()
    List<String> localRegistrations = []
    List<String> openIdRegistrations = []
    List<String> resetRequests = []

    @Override
    ApplicationResponse registerLocal(String fullName, String userName, String email,
        char[] password) {
      localRegistrations << email
      return localResponse
    }

    @Override
    ApplicationResponse registerOpenId(String fullName, String userName, String email,
        String issuer, String subject) {
      openIdRegistrations << email
      return openIdResponse
    }

    @Override
    ApplicationResponse requestPasswordReset(String email) {
      resetRequests << email
      return ApplicationResponse.successResponse()
    }
  }

  // ---- counters ---------------------------------------------------------------------------------

  /** Allows the first attempt, so the registration branch is reachable. */
  static class AllowingCounter implements AuthAttemptCounter {
    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      return new Counter(1, null)
    }

    @Override
    Counter peek(String scope, String key) { return new Counter(0, null) }

    @Override
    void block(String scope, String key, Instant blockedUntil) { }

    @Override
    void clear(String scope, String key) { }
  }

  /** Reports a live block, so the throttled branch is reachable. */
  static class BlockedCounter implements AuthAttemptCounter {
    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      return new Counter(99, now.plus(Duration.ofMinutes(15)))
    }

    @Override
    Counter peek(String scope, String key) {
      return new Counter(99, Instant.now().plus(Duration.ofMinutes(15)))
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) { }

    @Override
    void clear(String scope, String key) { }
  }
}
