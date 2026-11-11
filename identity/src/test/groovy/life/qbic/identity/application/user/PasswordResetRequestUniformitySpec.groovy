package life.qbic.identity.application.user

import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import life.qbic.identity.domain.event.PasswordResetRequested
import life.qbic.identity.domain.model.EmailAddress
import life.qbic.identity.domain.model.EncryptedPassword
import life.qbic.identity.domain.model.FullName
import life.qbic.identity.domain.model.User
import life.qbic.identity.domain.repository.UserDataStorage
import life.qbic.identity.domain.repository.UserRepository
import spock.lang.Specification

/**
 * <b>The reset request must not describe the account (AUTH-NFR-03, #1681).</b>
 *
 * <p>{@code requestPasswordReset} used to return a structured failure for the two cases an attacker
 * cares about — {@code UserNotFoundException("User not found")} and
 * {@code UserNotActivatedException("User not active")} — and the view rendered both verbatim. That
 * made the form an account-enumeration oracle: existence is the first step of every credential
 * attack, and "registered but unconfirmed" identifies the weaker target.</p>
 *
 * <p>Two things are asserted for each case, because fixing only one is not enough: the
 * <em>response</em> must be indistinguishable, and the <em>notification</em> must not go out. A fix
 * that returns success but still mails the unconfirmed address would disclose the same fact through
 * the mailbox instead of the screen.</p>
 */
class PasswordResetRequestUniformitySpec extends Specification {

  static final String KNOWN_ADDRESS = 'alice@example.org'
  static final String UNKNOWN_ADDRESS = 'nobody@example.org'

  List<DomainEvent> dispatched

  UserRepository repository
  IdentityService service

  def setup() {
    dispatched = []
    // Captured rather than really subscribed: the production subscribers enqueue JobRunr jobs, which
    // need infrastructure this spec deliberately does not start.
    DomainEventDispatcher.instance().subscribe(new CapturingSubscriber(dispatched))
  }

  def "an unknown address is indistinguishable from a known one"() {
    given:
    repository = repositoryWith([])
    service = new IdentityService(repository)

    when:
    def response = service.requestPasswordReset(UNKNOWN_ADDRESS)

    then: 'the caller is told it succeeded, exactly as for a real account'
    response.hasFailures() == false

    and: 'but nothing was dispatched, so no mail is sent'
    dispatched.isEmpty()
  }

  def "an unconfirmed account is indistinguishable from a known confirmed one"() {
    given:
    def unconfirmed = user(false)
    repository = repositoryWith([unconfirmed])
    service = new IdentityService(repository)

    when:
    def response = service.requestPasswordReset(KNOWN_ADDRESS)

    then: 'same response as any other well-formed address'
    response.hasFailures() == false

    and: 'no reset event, so no mail to a mailbox nobody has proven they control'
    dispatched.isEmpty()
  }

  def "a confirmed account does dispatch the reset"() {
    // The positive control. Without it, "uniform" could be satisfied by never sending anything.
    given:
    def confirmed = user(true)
    repository = repositoryWith([confirmed])
    service = new IdentityService(repository)

    when:
    def response = service.requestPasswordReset(KNOWN_ADDRESS)

    then:
    response.hasFailures() == false

    and:
    dispatched.size() == 1
    dispatched.first() instanceof PasswordResetRequested
  }

  def "the three outcomes are identical from the caller's point of view"() {
    given:
    def unknown = new IdentityService(repositoryWith([]))
    def unconfirmed = new IdentityService(repositoryWith([user(false)]))
    def confirmed = new IdentityService(repositoryWith([user(true)]))

    expect: 'no case reports a failure, so no caller can render a distinguishing message'
    !unknown.requestPasswordReset(UNKNOWN_ADDRESS).hasFailures()
    !unconfirmed.requestPasswordReset(KNOWN_ADDRESS).hasFailures()
    !confirmed.requestPasswordReset(KNOWN_ADDRESS).hasFailures()
  }

  def "a malformed address is still reported, because that is not an account-state question"() {
    // A visitor who mistyped their own address must be told. This discloses nothing about any
    // account: the answer depends only on the string they typed.
    given:
    service = new IdentityService(repositoryWith([]))

    when:
    def response = service.requestPasswordReset('not-an-email')

    then:
    response.hasFailures()
  }

  // ---- helpers -------------------------------------------------------------------------------

  private UserRepository repositoryWith(List<User> users) {
    def storage = Stub(UserDataStorage)
    storage.findUsersByEmailAddress(_) >> users
    return new UserRepository(storage)
  }

  private static User user(boolean active) {
    def created = User.create(FullName.from('Alice Example'), EmailAddress.from(KNOWN_ADDRESS),
        'alice', EncryptedPassword.from('aVeryGoodPassw0rd!'.toCharArray()))
    if (active) {
      created.activate()
    }
    return created
  }

  private static final class CapturingSubscriber implements
      DomainEventSubscriber<PasswordResetRequested> {

    private final List<DomainEvent> sink

    CapturingSubscriber(List<DomainEvent> sink) {
      this.sink = sink
    }

    @Override
    Class<? extends DomainEvent> subscribedToEventType() {
      return PasswordResetRequested
    }

    @Override
    void handleEvent(PasswordResetRequested event) {
      sink.add(event)
    }
  }
}
