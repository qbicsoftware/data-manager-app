package life.qbic.datamanager.security

import life.qbic.identity.api.OidcLinkService
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
 * <b>Linking writes an attribute and never a session (AUTH-R-07).</b>
 *
 * <p>The behaviour that matters most is an absence: after a link, the authenticated principal must be
 * the same one that started it. The previous arrangement could not promise that — {@code oauth2Login()}
 * saved an authenticated {@code OidcUser} into the session before the success handler ran, and the
 * handler had to mutate it back.</p>
 *
 * <p>The rest of this spec pins the two defects the old implementation carried: a link could not be
 * repaired (linking your own iD raised "OpenID already in use"), and it could not be removed at all.</p>
 */
class OidcLinkSpec extends Specification {

  static final String ISSUER = 'https://sandbox.orcid.org'
  static final String ID = '0000-0002-1825-0097'

  UserRepository userRepository
  OidcLinkService service

  def setup() {
    userRepository = Mock(UserRepository)
    // Default: no account holds the iD. Individual examples override this. Without the default an
    // unstubbed Optional-returning call yields null and the spec fails for the wrong reason.
    userRepository.findByOidc(_ as String, _ as String) >> Optional.empty()
    service = new OidcLinkServiceImpl(userRepository)
  }

  // ---- linking ---------------------------------------------------------------------------------

  def "linking stores the iD and issuer on the account"() {
    given:
    def user = account()
    userRepository.findById(user.id()) >> Optional.of(user)

    when:
    def outcome = service.link(user.id().get(), ID, ISSUER)

    then:
    outcome instanceof OidcLinkService.Outcome.Linked
    (outcome as OidcLinkService.Outcome.Linked).link().oidcId() == ID
    (outcome as OidcLinkService.Outcome.Linked).link().oidcIssuer() == ISSUER

    and: 'the account was updated exactly once'
    1 * userRepository.updateUser(user)
  }

  def "re-linking the iD the account already holds succeeds instead of failing"() {
    // The old implementation answered "OpenID already in use" here, which made a wrong or stale link
    // impossible to repair: a user could never re-run the link for their own account.
    given:
    def user = account(ID, ISSUER)
    userRepository.findById(user.id()) >> Optional.of(user)

    when:
    def outcome = service.link(user.id().get(), ID, ISSUER)

    then:
    outcome instanceof OidcLinkService.Outcome.AlreadyLinked

    and: 'nothing is written, because nothing changed'
    0 * userRepository.updateUser(_)
  }

  def "an iD held by another account is refused"() {
    given:
    def user = account()
    def other = account()
    userRepository.findById(user.id()) >> Optional.of(user)
    userRepository.findByOidc(ID, ISSUER) >> Optional.of(other)

    when:
    def outcome = service.link(user.id().get(), ID, ISSUER)

    then:
    outcome instanceof OidcLinkService.Outcome.HeldByAnotherAccount

    and: 'the refusal does not leak which account holds it'
    !outcome.toString().contains(other.id().get())
    userRepository.findByOidc(ID, ISSUER) >> Optional.of(other)

    and: 'nothing is written'
    0 * userRepository.updateUser(_)
  }

  @Unroll
  def "an iD in the form #candidate is rejected before anything is read or written"() {
    when:
    def outcome = service.link('some-user', candidate, ISSUER)

    then:
    outcome instanceof OidcLinkService.Outcome.InvalidIdentifier

    and: 'the account table is not consulted for an unusable value'
    0 * userRepository._

    where:
    candidate << [
        null,
        '',
        '   ',
        'not-an-orcid',
        'https://orcid.org/0000-0002-1825-0097',   // a URI, not the bare iD
        '0000-0002-1825-009',                      // too short
        '0000-0002-1825-00977',                    // too long
        '0000-0002-1825-009x',                     // lower-case check character
        "0000-0002-1825-0097\n",                   // trailing whitespace
    ]
  }

  def "linking an unknown account is reported, not thrown"() {
    given: 'no account with that id'
    userRepository.findById(_ as UserId) >> Optional.empty()

    when:
    def outcome = service.link('missing', ID, ISSUER)

    then:
    outcome instanceof OidcLinkService.Outcome.UnknownAccount
  }

  // ---- unlinking -------------------------------------------------------------------------------

  def "unlinking removes the iD and issuer"() {
    given:
    def user = account(ID, ISSUER)
    userRepository.findById(user.id()) >> Optional.of(user)

    when:
    def outcome = service.unlink(user.id().get())

    then:
    outcome instanceof OidcLinkService.Outcome.Unlinked

    and: 'the account no longer carries either half of the pair'
    user.oidcId() == null
    user.oidcIssuer() == null

    and:
    1 * userRepository.updateUser(user)
  }

  def "unlinking an account with no link is a no-op"() {
    given:
    def user = account()
    userRepository.findById(user.id()) >> Optional.of(user)

    when:
    def outcome = service.unlink(user.id().get())

    then:
    outcome instanceof OidcLinkService.Outcome.NotLinked
    0 * userRepository.updateUser(_)
  }

  def "unlink then relink works, so a mistaken link can be replaced"() {
    // The end-to-end reason unlink exists: a user who connected the wrong iD (or the right iD against
    // the wrong registry) must be able to correct it without a database edit.
    given:
    def user = account('0000-0002-1825-0097', ISSUER)
    userRepository.findById(user.id()) >> Optional.of(user)

    when: 'the wrong iD is removed'
    service.unlink(user.id().get())

    and: 'the right one is connected'
    def outcome = service.link(user.id().get(), '0000-0001-5000-0007', ISSUER)

    then:
    outcome instanceof OidcLinkService.Outcome.Linked
    user.oidcId() == '0000-0001-5000-0007'
  }

  // ---- reading ---------------------------------------------------------------------------------

  def "a link is read back as the stored pair"() {
    given:
    def user = account(ID, ISSUER)
    userRepository.findById(user.id()) >> Optional.of(user)

    expect:
    service.findLink(user.id().get()).get().oidcId() == ID
  }

  def "an account without a link reads back empty, not a pair of nulls"() {
    given:
    def user = account()
    userRepository.findById(user.id()) >> Optional.of(user)

    expect:
    service.findLink(user.id().get()).isEmpty()
  }

  // ---- helpers ---------------------------------------------------------------------------------

  /**
   * Builds a real account. A stub would be easier, but the behaviour under test includes the aggregate
   * mutating its own fields, so the object has to be real.
   */
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
