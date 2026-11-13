package life.qbic.datamanager.configuration

import org.springframework.mail.javamail.JavaMailSenderImpl
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicReference

/**
 * <b>A rejected mail login must stop the application, not be discovered by a user without a link.</b>
 *
 * <p>See {@link MailConnectionCheck} for the incident this answers: a confirmation email left a test
 * deployment unauthenticated and in cleartext while the application reported success, because Jakarta
 * Mail only <em>offers</em> credentials and a relay that does not want them accepts the message
 * anyway.</p>
 *
 * <p>These specs drive the check against {@link FakeSmtpServer}, a socket-level SMTP endpoint, rather
 * than a mock of {@link JavaMailSenderImpl}. That distinction is the point: the defect lived in the
 * interaction between the mail session and a real server's replies, and a mock would have asserted my
 * assumption about that interaction rather than the interaction itself. The plaintext-or-TLS
 * behaviour of {@code starttls.required} in particular cannot be observed any other way.</p>
 */
class MailConnectionCheckSpec extends Specification {

  FakeSmtpServer server

  def cleanup() {
    server?.close()
  }


  // ---- the gate refuses to start at all ---------------------------------------------------------

  def "a rejected login fails startup"() {
    given: 'a server that offers authentication and refuses it'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])
    server.expectedCredentials = ['user@example.org', 'the-right-password']

    when: 'the application starts with a stale password'
    new MailConnectionCheck(senderFor(server.port, 'user@example.org', 'a-stale-password'))

    then: 'the start fails, and the message says what is wrong rather than "mail server not available"'
    def failure = thrown(IllegalStateException)
    failure.message.contains('rejected the configured credentials')
    failure.message.contains('unauthenticated')
  }

  def "the password is never sent to a server that will not encrypt the channel"() {
    given: 'a relay that does not advertise STARTTLS — the incident, exactly'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])
    server.expectedCredentials = ['user@example.org', 'the-right-password']

    when: 'the check runs with starttls.required set'
    try {
      new MailConnectionCheck(senderFor(server.port, 'user@example.org', 'the-right-password'))
    } catch (IllegalStateException expected) {
      // Refused before authenticating; that is the point of this case.
    }

    then: 'no AUTH command was attempted at all'
    !server.sawAuthentication
    !server.commands.any { it.toUpperCase().startsWith('AUTH') }

    and: 'the failure is the TLS requirement, not the password'
    server.commands.any { it.toUpperCase().startsWith('EHLO') }
  }

  // ---- the second half of the incident: cleartext ------------------------------------------------

  def "a server that refuses to upgrade to TLS fails startup"() {
    given: 'a relay that does not advertise STARTTLS at all — the incident, exactly'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])

    and: 'credentials that are correct, so TLS is the only thing left to reject'
    server.expectedCredentials = ['user@example.org', 'the-right-password']

    when:
    new MailConnectionCheck(senderFor(server.port, 'user@example.org', 'the-right-password'))

    then: 'startup is refused, because the alternative is sending the token in cleartext'
    def failure = thrown(IllegalStateException)
    failure.message.contains('refused an encrypted connection')
  }

  def "the client demands STARTTLS when the server advertises it"() {
    given: 'a server that advertises STARTTLS and then refuses the upgrade'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED,
                                   FakeSmtpServer.Behaviour.STARTTLS_ADVERTISED])
    server.expectedCredentials = ['user@example.org', 'the-right-password']

    when:
    try {
      new MailConnectionCheck(senderFor(server.port, 'user@example.org', 'the-right-password'))
    } catch (IllegalStateException expected) {
      // Refused upgrade; asserted in the case above.
    }

    then: 'the client asked for the upgrade, so mail.smtp.starttls.required reached the session'
    server.commands.any { it.toUpperCase().startsWith('STARTTLS') }
  }

  def "without starttls.required the password IS sent over an unencrypted hop"() {
    given: 'the same relay, but only starttls.enable — the weaker setting'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])
    server.expectedCredentials = ['user@example.org', 'the-right-password']

    and: 'a sender configured the way a deployment without starttls.required is configured'
    def sender = senderFor(server.port, 'user@example.org', 'the-right-password')
    def lax = new Properties()
    lax.putAll(sender.getJavaMailProperties())
    lax.remove('mail.smtp.starttls.required')
    sender.setJavaMailProperties(lax)

    when:
    try {
      new MailConnectionCheck(sender)
    } catch (IllegalStateException expected) {
      // Not asserted on here.
    }

    then: 'the credentials were offered in the clear — which is why the required flag is the fix'
    server.sawAuthentication
  }

  // ---- configuration faults produce their own message, not a connect error -----------------------

  def "an empty username fails with a message that names the cause"() {
    given: 'no credentials configured: mail would be submitted unauthenticated'
    server = FakeSmtpServer.start([])

    when:
    new MailConnectionCheck(senderFor(server.port, '', ''))

    then: 'the failure explains the missing username instead of surfacing a connect error'
    def failure = thrown(IllegalStateException)
    failure.message.contains('spring.mail.username is empty')
    failure.message.contains('unauthenticated')
  }

  def "a blank host fails with a message that names the cause"() {
    given: 'no host configured, so there is nothing to connect to'
    def sender = new JavaMailSenderImpl()
    sender.setHost('')
    sender.setPort(25)
    sender.setUsername('user@example.org')
    sender.setPassword('pw')

    when:
    new MailConnectionCheck(sender)

    then: 'the failure names the missing host, rather than a connect error to 127.0.0.1:25'
    def failure = thrown(IllegalStateException)
    failure.message.contains('spring.mail.host is not set')
  }

  // ---- a dead server must not hang the start ----------------------------------------------------

  def "an unreachable server fails startup instead of blocking it"() {
    given: 'a port that accepts nothing, which is what a black-holed SMTP host looks like'
    def deadPort = findClosedPort()
    def before = System.currentTimeMillis()

    when:
    new MailConnectionCheck(senderFor(deadPort, 'user@example.org', 'pw'))

    then: 'the failure is prompt and explicit'
    def failure = thrown(IllegalStateException)
    failure.message.contains('Mail server')

    and: 'startup was not held open beyond the check deadline'
    System.currentTimeMillis() - before < 30_000
  }

  // ---- helpers ----------------------------------------------------------------------------------

  private static JavaMailSenderImpl senderFor(int port, String username, String password) {
    def sender = new JavaMailSenderImpl()
    sender.setHost('127.0.0.1')
    sender.setPort(port)
    sender.setUsername(username)
    sender.setPassword(password)
    def props = new Properties()
    props.setProperty('mail.smtp.auth', 'true')
    props.setProperty('mail.smtp.starttls.enable', 'true')
    props.setProperty('mail.smtp.starttls.required', 'true')
    // Keep the checks fast; the defaults would multiply every failing case by 60s+.
    props.setProperty('mail.smtp.connectiontimeout', '5000')
    props.setProperty('mail.smtp.timeout', '5000')
    props.setProperty('mail.smtp.writetimeout', '5000')
    sender.setJavaMailProperties(props)
    return sender
  }

  private static int findClosedPort() {
    def socket = new ServerSocket(0)
    def port = socket.localPort
    socket.close()
    return port
  }
}
