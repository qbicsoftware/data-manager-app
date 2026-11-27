package life.qbic.datamanager.views.account

import life.qbic.identity.application.mfa.InvalidTotpCodeException
import life.qbic.identity.application.mfa.MfaPolicy
import life.qbic.identity.application.mfa.PendingTotpEnrolment
import life.qbic.identity.application.mfa.SecondFactorSecretProtector
import life.qbic.identity.application.mfa.SecondFactorService
import life.qbic.identity.application.mfa.TotpEnrolmentService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.model.mfa.Totp
import life.qbic.identity.domain.repository.MfaGraceRepository
import life.qbic.identity.domain.repository.RecoveryCodeRepository
import life.qbic.identity.domain.repository.SecondFactorRepository
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Regression: the name the user typed must be the name in the roster</b>
 *
 * <p>Reported: "when providing a custom name, 'Authenticator app' is shown in the roster, which
 * confused people." The field is prefilled with the default, so a user who types their own label
 * expects to see it back.</p>
 *
 * <p>Confirmation happens in two steps — {@code confirm} creates the factor under the pending
 * enrolment's default label, then a separate {@code rename} applies the typed one — so the failure
 * mode to guard against is the second step being skipped while the first succeeded.</p>
 *
 * <p>The collaborators are the <b>real</b> services over in-memory repositories, not mocks: the
 * question is whether the sequence reaches the store, which is exactly what a mock would assume away.
 * </p>
 */
class EnrolmentChosenNameSpec extends Specification {

  static final String USER_ID = "u-1"
  static final String EMAIL = "ada@example.org"
  static final String SEED = Totp.newSeed()
  static final Duration WINDOW = Duration.ofMinutes(15)

  FakeFactors factors = new FakeFactors()
  FakeRecoveryCodes recoveryCodes = new FakeRecoveryCodes()
  TotpEnrolmentService enrolmentService
  SecondFactorService secondFactorService

  TwoFactorEnrolmentMain page
  PendingTotpEnrolment pending

  def setup() {
    def protector = new PlaintextProtector(SEED)
    def throttle = new AuthAttemptThrottle(new PermissiveCounter(),
        10, WINDOW, WINDOW, 5, WINDOW, WINDOW, 5, WINDOW, WINDOW)
    def encoder = new life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder(
        "p".repeat(life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder.MIN_PEPPER_LENGTH))

    enrolmentService = new TotpEnrolmentService(factors, protector)
    secondFactorService = new SecondFactorService(factors, recoveryCodes,
        new MfaPolicy("OPTIONAL", WINDOW, ""))
    def recoveryCodeService = new life.qbic.identity.application.mfa.RecoveryCodeService(
        recoveryCodes, encoder, throttle)

    def userInformation = Stub(life.qbic.identity.api.UserInformationService) {
      findById(USER_ID) >> Optional.of(new life.qbic.identity.api.UserInfo(
          USER_ID, "Ada", EMAIL, "ada", true, null, null))
    }

    // The page builds a RouterLink, which asks VaadinService for the router. Same scaffolding the
    // other view specs use (DeadLoginStateSpec); without it the constructor cannot run headless.
    def registry = Stub(com.vaadin.flow.server.RouteRegistry)
    registry.getTargetUrl(_, _ as com.vaadin.flow.router.RouteParameters) >> Optional.of("settings")
    def service = Stub(com.vaadin.flow.server.VaadinService)
    service.getRouter() >> new com.vaadin.flow.router.Router(registry)
    service.getContext() >> null
    com.vaadin.flow.server.VaadinService.setCurrent(service)

    page = new TwoFactorEnrolmentMain(enrolmentService, secondFactorService, recoveryCodeService,
        userInformation, Stub(AuthenticationToUserIdTranslationService))

    // Fifty codes so no recovery sheet is due; this spec is about the label.
    recoveryCodes.usable = 10
    pending = enrolmentService.begin(USER_ID, TwoFactorEnrolmentMain.DEFAULT_DISPLAY_NAME, EMAIL,
        Instant.now())
    page.beginForTest(pending)
  }

  // ---------------------------------------------------------------- the report

  def "a typed name is the name that reaches the store"() {
    given: "the user types their own label before verifying"
    page.setDisplayNameForTest("iPhone 13 Pro")

    when: "the code verifies"
    page.verifyCodeForTest(Totp.code(SEED, Instant.now()), EMAIL)

    then: "the factor exists under the label they typed, not the default"
    factors.stored.size() == 1
    factors.stored.first().displayName() == "iPhone 13 Pro"
  }

  def "the default label is stored when the field is untouched"() {
    when:
    page.verifyCodeForTest(Totp.code(SEED, Instant.now()), EMAIL)

    then:
    factors.stored.first().displayName() == TwoFactorEnrolmentMain.DEFAULT_DISPLAY_NAME
  }

  def "clearing the field stores the default rather than failing on a blank label"() {
    given: "the domain refuses a blank label, so the page must not send one"
    page.setDisplayNameForTest("   ")

    when:
    page.verifyCodeForTest(Totp.code(SEED, Instant.now()), EMAIL)

    then: "the factor exists, under the default"
    factors.stored.size() == 1
    factors.stored.first().displayName() == TwoFactorEnrolmentMain.DEFAULT_DISPLAY_NAME
  }

  def "a name is trimmed before it is stored"() {
    given:
    page.setDisplayNameForTest("  iPad mini  ")

    when:
    page.verifyCodeForTest(Totp.code(SEED, Instant.now()), EMAIL)

    then:
    factors.stored.first().displayName() == "iPad mini"
  }

  def "a wrong code creates no factor, so no name is stored"() {
    given:
    page.setDisplayNameForTest("iPhone 13 Pro")

    when:
    page.verifyCodeForTest("000000", EMAIL)

    then:
    factors.stored.isEmpty()
  }

  def cleanup() {
    com.vaadin.flow.server.VaadinService.setCurrent(null)
  }

  // ---------------------------------------------------------------- fakes

  static class PlaintextProtector implements SecondFactorSecretProtector {
    private final String seed

    PlaintextProtector(String seed) {
      this.seed = seed
    }

    @Override
    String protect(char[] plaintext) {
      // 60 bytes clears ProtectedSecret's nonce+tag floor; the value is opaque to this spec.
      return Base64.encoder.encodeToString(new byte[60])
    }

    @Override
    char[] reveal(String protectedSecret) {
      return seed.toCharArray()
    }
  }

  static class FakeFactors implements SecondFactorRepository {
    final List<SecondFactor> stored = []
    private long nextId = 42L

    @Override
    SecondFactor save(SecondFactor factor) {
      if (factor.id() == null) {
        // Hibernate assigns the surrogate id on persist, onto the SAME instance, and the adapter
        // returns that instance. Emulated by reflection because the field is private and set only by
        // JPA — and the rename path needs a real id, so a fake without one cannot exercise it.
        assignId(factor, nextId++)
        stored << factor
        return factor
      }
      stored.removeAll { it.id() == factor.id() }
      stored << factor
      return factor
    }

    private static void assignId(SecondFactor factor, long id) {
      def field = SecondFactor.getDeclaredField("id")
      field.setAccessible(true)
      field.set(factor, id)
    }

    @Override
    Optional<SecondFactor> findById(long id) {
      return Optional.ofNullable(stored.find { it.id() != null && it.id() == id })
    }

    @Override
    List<SecondFactor> findAllByUserId(String userId) {
      return stored.findAll { it.userId() == userId }
    }

    @Override
    List<SecondFactor> findActiveByUserId(String userId) {
      return stored.findAll { it.userId() == userId && it.isActive() }
    }

    @Override
    boolean hasActiveFactor(String userId) {
      return !findActiveByUserId(userId).isEmpty()
    }
  }

  static class FakeRecoveryCodes implements RecoveryCodeRepository {
    long usable = 0

    @Override
    List<life.qbic.identity.domain.model.mfa.RecoveryCode> saveAll(
        List<life.qbic.identity.domain.model.mfa.RecoveryCode> codes) {
      return codes
    }

    @Override
    List<life.qbic.identity.domain.model.mfa.RecoveryCode> findAllByUserId(String userId) {
      return []
    }

    @Override
    long countUsableByUserId(String userId) {
      return usable
    }

    @Override
    int invalidateAllFor(String userId, Instant now) {
      return 0
    }

    @Override
    Optional<life.qbic.identity.domain.model.mfa.RecoveryCode> consumeIfUsable(
        String codeHash, Instant now) {
      return Optional.empty()
    }

    @Override
    Optional<life.qbic.identity.domain.model.mfa.RecoveryCode> consumeIfUsableForUser(
        String codeHash, String userId, Instant now) {
      return Optional.empty()
    }
  }

  static class PermissiveCounter implements life.qbic.identity.domain.repository.AuthAttemptCounter {
    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      return new Counter(1, null)
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
    }

    @Override
    Counter peek(String scope, String key) {
      return new Counter(0, null)
    }

    @Override
    void clear(String scope, String key) {
    }
  }
}
