package life.qbic.datamanager.configuration

import org.springframework.mail.javamail.JavaMailSenderImpl
import spock.lang.Specification
import spock.lang.Unroll

import java.util.Properties

/**
 * <b>Mail submission must be authenticated and encrypted — and the configuration must say so.</b>
 *
 * <p>Found from a delivered message, not from the suite. A confirmation email arrived at an external
 * mailbox with this hop in its headers:</p>
 *
 * <pre>
 * Received: from 192.168.178.48 (unknown [134.2.250.7])
 *     by mx03.uni-tuebingen.de (Postfix) with ESMTP id A718220C4846
 * </pre>
 *
 * <p>Three things are wrong with that line: {@code ESMTP} rather than {@code ESMTPS} means the hop
 * was <em>unencrypted</em>; the recipient was an external domain, so the mail was <em>relayed</em>,
 * not merely stored; and the mail password in the deployment was stale, so <em>no authentication
 * succeeded</em> either. The application reported success throughout.</p>
 *
 * <h3>Why the application cannot catch this itself</h3>
 *
 * <p>Spring's {@link JavaMailSenderImpl} does fail on a rejected login: {@code doSend} calls
 * {@code connectTransport()}, which passes the credentials to {@code Transport.connect(host, port,
 * user, password)} and rethrows {@code AuthenticationFailedException} as {@code
 * MailAuthenticationException} <em>before</em> any message is sent. What it cannot do is refuse a
 * server that never asked for credentials — Jakarta Mail only <em>offers</em> them. If the relay
 * accepts unauthenticated mail, the send succeeds and nothing signals that the hop was insecure.</p>
 *
 * <p>So the control is the configuration, and the configuration has two silent-failure modes worth
 * knowing:</p>
 *
 * <ol>
 *   <li><b>No {@code spring.mail.properties.mail.smtp.*} at all.</b> Before this change there were
 *       none, so nothing requested TLS and nothing required authentication.</li>
 *   <li><b>A misspelled key.</b> {@code spring.mail.properties} is a plain map; a typo such as
 *       {@code mail.smtp.starttls_enable} is not rejected, it is passed through to the mail session
 *       where it is ignored. {@code application.properties} had exactly this class of bug in the same
 *       block (two lines began with a space, which is harmless — {@code Properties.load} trims it —
 *       but the pattern is the same: the file looked right and was not checked).</li>
 * </ol>
 *
 * <p>These specs read the properties through the same relaxed binder Spring Boot uses, and assert on
 * the keys that actually reach the mail session.</p>
 */
class MailSubmissionSecuritySpec extends Specification {

  /** The three mail settings that decide whether a submission hop is authenticated and encrypted. */
  private static final List<String> REQUIRED_SMTP_PROPERTIES = [
      'mail.smtp.auth',
      'mail.smtp.starttls.enable',
      'mail.smtp.starttls.required',
  ]

  // ---- the shipped configuration ----------------------------------------------------------------

  def "the shipped configuration requires authentication and TLS for submission"() {
    given: 'the properties the application actually ships'
    def properties = loadProperties('datamanager-app/src/main/resources/application.properties')

    expect: 'every control is present, so a deployment that inherits defaults is not insecure'
    REQUIRED_SMTP_PROPERTIES.every { properties.getProperty(smtpPropertyKey(it)) == 'true' }
  }

  def "the distribution template requires authentication and TLS for submission"() {
    given: 'the template an administrator copies and edits'
    def properties = loadProperties('application.properties.template')

    expect: 'a deployment built from the blueprint cannot silently inherit an unauthenticated hop'
    REQUIRED_SMTP_PROPERTIES.every { properties.getProperty(smtpPropertyKey(it)) == 'true' }
  }

  @Unroll
  def "the shipped configuration sets #key to a plain value, not a mangled one"() {
    given: 'a leading space turns `key=value` into a space-separated pair; the value then keeps the `=`'
    def properties = loadProperties('datamanager-app/src/main/resources/application.properties')

    expect:
    properties.getProperty(key) == expected

    where:
    key                            | expected
    'spring.mail.port'             | '587'
    'spring.mail.default-encoding' | 'UTF-8'
  }

  // ---- what the mail session actually receives --------------------------------------------------

  def "the SMTP properties reach the JavaMail session, not just the properties file"() {
    given: 'the configuration the application ships, bound onto a real JavaMailSenderImpl'
    def sender = senderFor(loadProperties('datamanager-app/src/main/resources/application.properties'))

    when: 'the session is built the way the mail auto-configuration builds it'
    def session = sender.getSession()

    then: 'authentication is requested'
    session.getProperty('mail.smtp.auth') == 'true'

    and: 'STARTTLS is both offered and required, so a server without it is a failure, not a fallback'
    session.getProperty('mail.smtp.starttls.enable') == 'true'
    session.getProperty('mail.smtp.starttls.required') == 'true'

    and: 'the credentials the session will offer are the configured ones'
    sender.getUsername() == '\${MAIL_USERNAME}'
    sender.getPort() == 587
  }

  def "a misspelled smtp property reaches the session under the wrong name and is then ignored"() {
    given: 'the map is unvalidated: JavaMail reads what it recognises and silently drops the rest'
    def mangled = new Properties()
    mangled.setProperty('spring.mail.host', 'smtp.example.org')
    mangled.setProperty('spring.mail.port', '587')
    mangled.setProperty('spring.mail.properties.mail.smtp.starttls_enable', 'true')

    when:
    def session = senderFor(mangled).getSession()

    then: 'the typo is carried along verbatim...'
    session.getProperty('mail.smtp.starttls_enable') == 'true'

    and: '...while the key JavaMail actually reads is absent, so TLS is silently off'
    session.getProperty('mail.smtp.starttls.enable') == null

    and: 'and authentication, which was never configured, is off too'
    session.getProperty('mail.smtp.auth') == null
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private static String smtpPropertyKey(String smtpProperty) {
    return "spring.mail.properties.$smtpProperty"
  }

  /**
   * Loads a properties file the way Spring Boot does, with a leading space already trimmed by
   * {@link Properties#load}. Kept as a real load rather than a text scan so the assertions see
   * exactly what the application sees.
   */
  private static Properties loadProperties(String path) {
    def candidates = [new File(path), new File("../$path"), new File("../../$path")]
    def file = candidates.find { it.exists() }
    assert file != null : "$path not found from ${new File('.').absolutePath}"
    def properties = new Properties()
    file.withInputStream { properties.load(it) }
    return properties
  }

  /**
   * Binds {@code spring.mail.*} onto a real {@link JavaMailSenderImpl}, the way the mail
   * auto-configuration does, so the assertions see the mail session the application would build.
   *
   * <p>Going through the sender rather than constructing a {@code Session} by hand matters: binding
   * and session construction are the two places a misspelled {@code spring.mail.properties.*} key can
   * be dropped, and only the real type reproduces both.</p>
   */
  private static JavaMailSenderImpl senderFor(Properties properties) {
    def prefix = 'spring.mail.'
    def sender = new JavaMailSenderImpl()
    def smtp = new Properties()
    properties.stringPropertyNames()
        .findAll { it.startsWith(prefix) }
        .each { key ->
          def suffix = key.substring(prefix.length())
          if (suffix.startsWith('properties.')) {
            smtp.setProperty(suffix.substring('properties.'.length()), properties.getProperty(key))
          } else if (suffix == 'host') {
            sender.setHost(properties.getProperty(key))
          } else if (suffix == 'port') {
            sender.setPort(Integer.parseInt(properties.getProperty(key)))
          } else if (suffix == 'username') {
            sender.setUsername(properties.getProperty(key))
          } else if (suffix == 'password') {
            sender.setPassword(properties.getProperty(key))
          } else if (suffix == 'default-encoding') {
            sender.setDefaultEncoding(properties.getProperty(key))
          }
        }
    sender.setJavaMailProperties(smtp)
    return sender
  }
}
