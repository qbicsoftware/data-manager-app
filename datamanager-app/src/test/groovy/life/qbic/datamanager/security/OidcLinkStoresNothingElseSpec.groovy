package life.qbic.datamanager.security

import life.qbic.identity.api.OidcLinkOutcomeMessage
import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
import life.qbic.identity.domain.model.UserId
import life.qbic.identity.domain.repository.UserRepository
import life.qbic.identity.application.user.OidcLinkServiceImpl
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>A link writes two columns and nothing else.</b>
 *
 * <p>The old ORCID flow kept an access token alive after the code exchange, and it parked an
 * <code>Authentication</code> in the session so a handler could restore it afterwards. Both are gone, and
 * "gone" is only meaningful if it is asserted: a token left in an
 * <code>OAuth2AuthorizedClient</code> turns the application into a holder of provider credentials, and a
 * principal parked in a session is the state second-factor enforcement has to be able to rule out.</p>
 *
 * <p>The session-side half of that is asserted in {@code OidcLinkOutcomeRenderingSpec} (which store) and
 * here (what is written to the account). The token half is structural, because it lives in the filter.</p>
 */
class OidcLinkStoresNothingElseSpec extends Specification {

  static final String ISSUER = 'https://orcid.org'
  static final String ID = '0000-0002-1825-0097'
  static final String FILTER =
      'datamanager-app/src/main/java/life/qbic/datamanager/security/OidcLinkCompletionFilter.java'

  UserRepository userRepository
  OidcLinkServiceImpl service

  def setup() {
    userRepository = Mock(UserRepository)
    userRepository.findByOidc(_ as String, _ as String) >> Optional.empty()
    service = new OidcLinkServiceImpl(userRepository)
  }

  // ---- what is written to the account ---------------------------------------------------------

  def "a successful link writes the iD and the issuer, and the account holds nothing else new"() {
    given:
    def user = account()
    userRepository.findById(user.id()) >> Optional.of(user)
    def before = [user.fullName(), user.emailAddress(), user.encryptedPassword, user.isActive()]

    when:
    service.link(user.id().get(), ID, ISSUER)

    then: 'the two link columns are set'
    user.oidcId() == ID
    user.oidcIssuer() == ISSUER

    and: 'nothing else about the account changed'
    [user.fullName(), user.emailAddress(), user.encryptedPassword, user.isActive()] == before
  }

  def "linking does not touch the email address, which is the authentication identifier"() {
    // AUTH-R-06: exactly one identifier, the verified email. The ORCID record's email is an unverified
    // attribute of a public profile; overwriting the local one with it would let whoever controls an
    // ORCID record repoint an account to their own mailbox and then use "forgot your password".
    given:
    def user = account()
    userRepository.findById(user.id()) >> Optional.of(user)

    when:
    service.link(user.id().get(), ID, ISSUER)

    then:
    user.emailAddress().get() == 'ada@example.org'
  }

  def "unlinking clears both halves, so neither column can be matched on afterwards"() {
    given:
    def user = account(ID, ISSUER)
    userRepository.findById(user.id()) >> Optional.of(user)

    when:
    service.unlink(user.id().get())

    then:
    user.oidcId() == null
    user.oidcIssuer() == null
  }

  // ---- what is not kept from the provider -----------------------------------------------------

  def "the completion filter discards the provider token after reading the iD"() {
    given: 'the filter source with comments stripped, so this is about code'
    def code = new File(FILTER).exists() ? new File(FILTER).text
        : new File("../$FILTER").text
    def lines = code.readLines()
        .findAll { !it.strip().startsWith('*') && !it.strip().startsWith('//') }
        .join('\n')

    expect: 'the authorized client is removed'
    lines.contains('removeAuthorizedClient')

    and: 'the removal is not conditional on success, so a failure cannot leave a live token behind'
    lines.contains('finally')
  }

  def "the link filter never writes a principal to the session"() {
    given: 'the filter source with comments stripped'
    def source = new File(FILTER).exists() ? new File(FILTER).text
        : new File("../$FILTER").text
    def code = source.readLines()
        .findAll { !it.strip().startsWith('*') && !it.strip().startsWith('//') }
        .join('\n')

    expect: 'no authentication is created, set, or stored'
    !code.contains('setAuthentication')
    !code.contains('new UsernamePasswordAuthenticationToken')
    !code.contains('SecurityContextHolder.getContext().setAuthentication')

    and: 'it does not write the pending-link marker either — the controller owns that'
    !code.contains('setAttribute(OidcLinkController.PENDING_LINK')
  }

  // ---- the outcome set stays closed ------------------------------------------------------------

  @Unroll
  def "outcome #outcome is a value the view can render"() {
    expect: 'every outcome has a message key, so a new one cannot ship silently unrendered'
    outcome.messageKey() == outcome.name().toLowerCase()
    OidcLinkOutcomeMessage.values().contains(outcome)

    where:
    outcome << OidcLinkOutcomeMessage.values().toList()
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private static User account(String oidcId = null, String oidcIssuer = null) {
    def user = User.create(FullName.from('Ada Lovelace'),
        EmailAddress.from('ada@example.org'), 'alovelace',
        EncryptedPassword.from('a-strong-passphrase'.toCharArray()))
    if (oidcId != null) {
      user.setOidcEntry(oidcIssuer, oidcId)
    }
    return user
  }
}
