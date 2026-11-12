package life.qbic.datamanager.security

import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.api.UserPassword
import life.qbic.identity.api.UserPasswordService
import life.qbic.identity.application.security.QBiCPasswordEncoder
import life.qbic.identity.domain.model.PasswordEncryptionPolicy
import life.qbic.projectmanagement.application.authorization.QbicUserDetails
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.userdetails.UsernameNotFoundException
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Login failures must not disclose whether an account exists (AUTH-NFR-03, #1681).</b>
 *
 * <p>These specs cover the two distinct concerns in {@link UserDetailsServiceImpl}: the
 * <em>outcome</em> (which is this class's responsibility) and the <em>timing</em> (which Spring
 * Security already equalises, and which this class must therefore not disturb by adding work of its
 * own). Both are asserted, because the tempting fix — "compare against a dummy hash here" — breaks
 * the second while appearing to fix the first.</p>
 */
class UserDetailsServiceImplSpec extends Specification {

  private static final String UNKNOWN_ADDRESS = "nobody@example.org"
  private static final String KNOWN_ADDRESS = "alice@example.org"
  private static final String USER_ID = "3f3e2b1a-0000-4000-8000-000000000000"

  UserAuthorityProvider authorityProvider = Stub(UserAuthorityProvider) {
    getAuthoritiesByUserId(_) >> []
  }

  QBiCPasswordEncoder realEncoder = new QBiCPasswordEncoder()

  def "an unknown address is rejected, and it is rejected as 'no such user' so the provider can hide it"() {
    given:
    def service = new UserDetailsServiceImpl(
        Stub(UserInformationService) { findByEmail(_) >> Optional.empty() },
        Stub(UserPasswordService),
        authorityProvider)

    when:
    service.loadUserByUsername(UNKNOWN_ADDRESS)

    then:
    // UsernameNotFoundException specifically: DaoAuthenticationProvider converts exactly this type
    // into BadCredentialsException (hideUserNotFoundExceptions defaults to true), so the visitor
    // sees the same "Incorrect username or password" as for a wrong password.
    thrown(UsernameNotFoundException)
  }

  def "an account without a stored password yields a password that can never match, not null"() {
    given:
    def service = new UserDetailsServiceImpl(
        Stub(UserInformationService) {
          findByEmail(KNOWN_ADDRESS) >> Optional.of(userInfo(false))
        },
        // This is the ORCID-only case (AUTH-R-07 / #1683) and also any account row whose
        // encryptedPassword column is null.
        Stub(UserPasswordService) { findEncryptedPasswordForUser(_) >> Optional.empty() },
        authorityProvider)

    when:
    QbicUserDetails details = service.loadUserByUsername(KNOWN_ADDRESS) as QbicUserDetails

    then: 'the stored password is a well-formed value, so the provider performs a real derivation'
    details.password == UserDetailsServiceImpl.NO_PASSWORD
    details.password.count(':') == 2

    and: 'no password matches it — including one equal to the stand-in itself'
    !realEncoder.matches('', UserDetailsServiceImpl.NO_PASSWORD)
    !realEncoder.matches('userNotFoundPassword', UserDetailsServiceImpl.NO_PASSWORD)
    !realEncoder.matches('any-password-at-all', UserDetailsServiceImpl.NO_PASSWORD)
    !realEncoder.matches(UserDetailsServiceImpl.NO_PASSWORD, UserDetailsServiceImpl.NO_PASSWORD)
  }

  def "the no-password stand-in parses under the real encryption policy, so it cannot fail fast"() {
    // Guards the subtle failure mode: if NO_PASSWORD were malformed, doPasswordsMatch would throw
    // instead of deriving, and an ORCID-only account would cost ~0 ms while a wrong password costs
    // ~30 ms. The timing oracle would be back without any test failing on the outcome.
    when:
    PasswordEncryptionPolicy.instance()
        .doPasswordsMatch('anything'.toCharArray(), UserDetailsServiceImpl.NO_PASSWORD)

    then:
    noExceptionThrown()
  }

  def "a known account with a password is returned unchanged"() {
    given:
    def encoded = realEncoder.encode('correct horse battery staple')
    def service = new UserDetailsServiceImpl(
        Stub(UserInformationService) {
          findByEmail(KNOWN_ADDRESS) >> Optional.of(userInfo(true))
        },
        Stub(UserPasswordService) {
          findEncryptedPasswordForUser(USER_ID) >> Optional.of(new UserPassword(USER_ID, encoded))
        },
        authorityProvider)

    when:
    QbicUserDetails details = service.loadUserByUsername(KNOWN_ADDRESS) as QbicUserDetails

    then:
    details.password == encoded
    details.userId == USER_ID
    realEncoder.matches('correct horse battery staple', details.password)
  }

  def "an unconfirmed account still reports its active flag, so the pre-check can reject it"() {
    given:
    def service = new UserDetailsServiceImpl(
        Stub(UserInformationService) {
          findByEmail(KNOWN_ADDRESS) >> Optional.of(userInfo(false))
        },
        Stub(UserPasswordService) {
          findEncryptedPasswordForUser(USER_ID) >>
              Optional.of(new UserPassword(USER_ID, realEncoder.encode('secret-password')))
        },
        authorityProvider)

    when:
    QbicUserDetails details = service.loadUserByUsername(KNOWN_ADDRESS) as QbicUserDetails

    then:
    !details.enabled
  }

  def "through the real provider, an unknown address and a wrong password fail with the same exception"() {
    // This is the end-to-end assertion the acceptance criterion asks for: "the message is identical
    // to the wrong-password case for a known account". It runs the actual DaoAuthenticationProvider
    // that Spring Boot wires from the UserDetailsService bean, so it covers the provider's own
    // hiding of UsernameNotFoundException rather than assuming it.
    given:
    def encoded = realEncoder.encode('the-real-password')
    def knownService = new UserDetailsServiceImpl(
        Stub(UserInformationService) {
          findByEmail(KNOWN_ADDRESS) >> Optional.of(userInfo(true))
        },
        Stub(UserPasswordService) {
          findEncryptedPasswordForUser(USER_ID) >> Optional.of(new UserPassword(USER_ID, encoded))
        },
        authorityProvider)
    def unknownService = new UserDetailsServiceImpl(
        Stub(UserInformationService) { findByEmail(_) >> Optional.empty() },
        Stub(UserPasswordService),
        authorityProvider)

    and:
    def knownProvider = providerFor(knownService)
    def unknownProvider = providerFor(unknownService)

    when:
    Throwable knownFailure = failureOf(knownProvider, KNOWN_ADDRESS, 'wrong-password')
    Throwable unknownFailure = failureOf(unknownProvider, UNKNOWN_ADDRESS, 'wrong-password')

    then: 'both are BadCredentialsException — not UsernameNotFoundException for one of them'
    knownFailure instanceof BadCredentialsException
    unknownFailure instanceof BadCredentialsException

    and: 'and the message a user would see is byte-for-byte the same'
    knownFailure.message == unknownFailure.message
  }

  def "an ORCID-only account cannot log in with any password, and does not 500"() {
    // Regression guard for the null stored password: the provider used to NPE on null.split(":"),
    // which surfaced as a 500 rather than a failed login.
    given:
    def service = new UserDetailsServiceImpl(
        Stub(UserInformationService) {
          findByEmail(KNOWN_ADDRESS) >> Optional.of(userInfo(true))
        },
        Stub(UserPasswordService) { findEncryptedPasswordForUser(_) >> Optional.empty() },
        authorityProvider)
    def provider = providerFor(service)

    when:
    Throwable failure = failureOf(provider, KNOWN_ADDRESS, 'anything')

    then:
    failure instanceof BadCredentialsException
    !(failure instanceof NullPointerException)
  }

  @Unroll
  def "the no-password stand-in is rejected for input #description"() {
    expect:
    !realEncoder.matches(input, UserDetailsServiceImpl.NO_PASSWORD)

    where:
    description       | input
    'empty'           | ''
    'the salt only'   | '0000000000000000000000000000000000000000'
    'the Spring dummy'| 'userNotFoundPassword'
    'a plausible one' | 'Password1234!'
  }

  // ---- helpers -------------------------------------------------------------------------------

  private static UserInfo userInfo(boolean active) {
    new UserInfo(USER_ID, 'Alice Example', KNOWN_ADDRESS, 'alice', active, null, null)
  }

  private DaoAuthenticationProvider providerFor(UserDetailsServiceImpl service) {
    def provider = new DaoAuthenticationProvider(service)
    provider.setPasswordEncoder(realEncoder)
    provider.afterPropertiesSet()
    return provider
  }

  private static Throwable failureOf(DaoAuthenticationProvider provider, String username,
      String password) {
    try {
      provider.authenticate(new UsernamePasswordAuthenticationToken(username, password))
      return null
    } catch (Throwable t) {
      return t
    }
  }
}
