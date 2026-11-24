package life.qbic.infrastructure.email

import life.qbic.identity.application.communication.CommunicationException as IdentityCommunicationException
import life.qbic.identity.application.communication.Content as IdentityContent
import life.qbic.identity.application.communication.EmailService as IdentityEmailService
import life.qbic.identity.application.communication.Recipient as IdentityRecipient
import life.qbic.identity.application.communication.Subject as IdentitySubject
import life.qbic.infrastructure.email.identity.IdentityEmailServiceProvider
import life.qbic.infrastructure.email.project.ProjectManagementEmailServiceProvider
import life.qbic.infrastructure.email.usergroups.UserGroupsEmailServiceProvider
import life.qbic.projectmanagement.application.communication.CommunicationException as ProjectCommunicationException
import life.qbic.usergroups.application.communication.CommunicationException as UserGroupsCommunicationException
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import spock.lang.Specification

/**
 * <b>A failed submission must be logged, translated, and handed to the caller as the type its port
 * promises.</b>
 *
 * <p>This spec exists because of a real incident that looked like a broken release: password-reset
 * mails stopped arriving after 1.20.4, a new token appeared in the database on every attempt, and
 * there was nothing in the log. The SMTP server had been briefly unavailable and the mails arrived
 * later.</p>
 *
 * <h3>Defect 1 — the failure was invisible</h3>
 *
 * <p>{@link EmailServiceProvider} used to submit with {@code Transport.send(message)} and therefore
 * declared {@code catch (MessagingException)}. Commit {@code 3b850c48} moved submission to Spring's
 * {@code JavaMailSender} and changed the call, but left the handler. The families are unrelated —
 * {@code MailException} does <b>not</b> extend {@code MessagingException}:</p>
 *
 * <pre>
 *   jakarta.mail.MessagingException        assembly  (e.g. AddressException)
 *   org.springframework.mail.MailException submission (MailSendException,
 *                                                      MailAuthenticationException)
 * </pre>
 *
 * <p>From that commit onward every genuine submission failure escaped <em>unlogged</em>: no
 * {@code log.error}, no translation, nothing to grep for. Only the background job noticed, by
 * quietly retrying.</p>
 *
 * <h3>Defect 2 — the exception crossed a context boundary</h3>
 *
 * <p>The identity, project-management and user-groups contexts each define their <b>own</b>
 * {@code CommunicationException}. Three distinct classes that merely share a name. The provider is
 * shared infrastructure, so when it threw <em>identity's</em> type, the other two adapters' catch
 * blocks could never fire, and a caller catching the type its port declared received nothing.</p>
 *
 * <p>The provider now throws {@link EmailSubmissionException} — context-free, which is what its
 * signature always declared — and each adapter maps that onto its own context's type.</p>
 *
 * <h3>Why the assertion is on the exact class</h3>
 *
 * <p>An earlier version of this spec asserted {@code it.class.simpleName == 'CommunicationException'}
 * across all three contexts. That cannot distinguish the three types, so it passed while the
 * layering was wrong. Every case below asserts the <b>fully-qualified</b> type instead; that is the
 * whole point of the spec, and matching on a simple name defeats it.</p>
 */
class EmailFailureIsVisibleSpec extends Specification {

  // ---- the provider throws its own, context-free type -------------------------------------------

  def "the provider throws EmailSubmissionException, as its signature declares"() {
    given:
    def sender = deadSender(new MailSendException('connection refused'))
    def provider = new EmailServiceProvider(sender)

    when:
    provider.send(new Subject('subject'), new Recipient('someone@example.org', 'Some One'),
        new Content('body'))

    then: 'the declared type is the thrown type - not a context type borrowed from identity'
    thrown(EmailSubmissionException)
  }

  def "a rejected login is translated too"() {
    given: 'a stale password surfaces as MailAuthenticationException - the other submission failure'
    def sender = deadSender(new MailAuthenticationException('535 5.7.8'))

    when:
    new EmailServiceProvider(sender).send(
        new Subject('subject'), new Recipient('someone@example.org', 'Some One'),
        new Content('body'))

    then:
    thrown(EmailSubmissionException)
  }

  def "a submission failure while sending an attachment is translated as well"() {
    given: 'the attachment overload has its own try block, so it needs its own guard'
    def sender = deadSender(new MailSendException('read timed out'))

    when:
    new EmailServiceProvider(sender).send(
        new Subject('subject'), new Recipient('someone@example.org', 'Some One'),
        new Content('body'), new Attachment('report.txt', 'payload'))

    then:
    thrown(EmailSubmissionException)
  }

  // ---- each adapter hands the caller the type its own port declares -----------------------------

  def "the identity adapter throws identity's CommunicationException"() {
    given:
    def provider = new EmailServiceProvider(deadSender(new MailSendException('refused')))
    IdentityEmailService send = new IdentityEmailServiceProvider(provider)

    when:
    send.send(new IdentitySubject('s'), new IdentityRecipient('someone@example.org', 'Some One'),
        new IdentityContent('c'))

    then:
    def failure = thrown(IdentityCommunicationException)
    failure.cause instanceof EmailSubmissionException
  }

  def "the project-management adapter throws project-management's CommunicationException"() {
    given:
    def provider = new EmailServiceProvider(deadSender(new MailSendException('refused')))
    def send = new ProjectManagementEmailServiceProvider(provider)

    when:
    send.send(new life.qbic.projectmanagement.application.communication.Subject('s'),
        new life.qbic.projectmanagement.application.communication.Recipient(
            'someone@example.org', 'Some One'),
        new life.qbic.projectmanagement.application.communication.Content('c'))

    then:
    def failure = thrown(ProjectCommunicationException)
    failure.cause instanceof EmailSubmissionException
  }

  def "the user-groups adapter throws user-groups' CommunicationException"() {
    given:
    def provider = new EmailServiceProvider(deadSender(new MailSendException('refused')))
    def send = new UserGroupsEmailServiceProvider(provider)

    when:
    send.send(new life.qbic.usergroups.application.communication.Subject('s'),
        new life.qbic.usergroups.application.communication.Recipient(
            'someone@example.org', 'Some One'),
        new life.qbic.usergroups.application.communication.Content('c'))

    then:
    def failure = thrown(UserGroupsCommunicationException)
    failure.cause instanceof EmailSubmissionException
  }

  def "the three adapters throw three distinct exception types, not one shared name"() {
    given: 'each context defines its own type; a shared name is exactly the trap this guards'
    def provider = new EmailServiceProvider(deadSender(new MailSendException('refused')))

    when: 'each context sends against a dead server'
    def failures = []
    try {
      new IdentityEmailServiceProvider(provider).send(new IdentitySubject('s'),
          new IdentityRecipient('someone@example.org', 'Some One'), new IdentityContent('c'))
    } catch (RuntimeException e) { failures << e }
    try {
      new ProjectManagementEmailServiceProvider(provider).send(
          new life.qbic.projectmanagement.application.communication.Subject('s'),
          new life.qbic.projectmanagement.application.communication.Recipient(
              'someone@example.org', 'Some One'),
          new life.qbic.projectmanagement.application.communication.Content('c'))
    } catch (RuntimeException e) { failures << e }
    try {
      new UserGroupsEmailServiceProvider(provider).send(
          new life.qbic.usergroups.application.communication.Subject('s'),
          new life.qbic.usergroups.application.communication.Recipient(
              'someone@example.org', 'Some One'),
          new life.qbic.usergroups.application.communication.Content('c'))
    } catch (RuntimeException e) { failures << e }

    then: 'all three are CommunicationException by simple name...'
    failures.size() == 3
    failures.every { it.class.simpleName == 'CommunicationException' }

    and: '...but three different classes, so each caller catches what its port promises'
    def thrownTypes = failures.collect { it.class.name } as Set<String>
    thrownTypes.size() == 3
    thrownTypes == ([IdentityCommunicationException.name,
                     ProjectCommunicationException.name,
                     UserGroupsCommunicationException.name] as Set<String>)
  }

  def "the project-management adapter translates failures from its attachment overload"() {
    given: 'the attachment overload has its own try block, so it needs its own guard'
    def provider = new EmailServiceProvider(deadSender(new MailSendException('refused')))
    def send = new ProjectManagementEmailServiceProvider(provider)

    when:
    send.send(new life.qbic.projectmanagement.application.communication.Subject('s'),
        new life.qbic.projectmanagement.application.communication.Recipient(
            'someone@example.org', 'Some One'),
        new life.qbic.projectmanagement.application.communication.Content('c'),
        new life.qbic.projectmanagement.application.communication.Attachment('payload', 'report.txt'))

    then: 'project-management\'s type, with the infrastructure exception as cause'
    def failure = thrown(ProjectCommunicationException)
    failure.cause instanceof EmailSubmissionException
  }

  // ---- helpers ---------------------------------------------------------------------------------

  /**
   * A sender whose transport always fails, with the given Spring mail exception.
   *
   * <p>An instance method, not {@code static}: Spock can only create {@code Mock}s inside a
   * specification instance, so a static helper fails to compile with "Mocks cannot be created in
   * static scope".</p>
   */
  private JavaMailSenderImpl deadSender(RuntimeException failure) {
    def sender = Mock(JavaMailSenderImpl)
    sender.createMimeMessage() >> new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null)
    sender.send(_ as jakarta.mail.internet.MimeMessage) >> { throw failure }
    return sender
  }
}
