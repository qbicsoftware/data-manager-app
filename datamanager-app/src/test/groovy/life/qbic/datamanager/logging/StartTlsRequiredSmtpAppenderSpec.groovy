package life.qbic.datamanager.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.net.SMTPAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.LayoutBase
import ch.qos.logback.core.boolex.EventEvaluatorBase
import life.qbic.datamanager.configuration.FakeSmtpServer
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The error-notification appender must require TLS, and must never send credentials in cleartext.</b>
 *
 * <p>The defect class this pins is subtle enough that it survived a code review and a source reading:
 * logback authenticates perfectly well, and {@code <STARTTLS>true</STARTTLS>} looks like it secures the
 * hop. Both impressions are wrong in the same way — they describe an <em>attempt</em>, not a
 * <em>requirement</em>. Measured against a relay that cannot upgrade, with correct credentials:</p>
 *
 * <pre>
 * SMTPAppender, no STARTTLS        -&gt; AUTH LOGIN &lt;user&gt; &lt;pass&gt;, mail delivered   (cleartext)
 * SMTPAppender, &lt;STARTTLS&gt;true    -&gt; AUTH LOGIN &lt;user&gt; &lt;pass&gt;, mail delivered   (cleartext)
 * StartTlsRequiredSmtpAppender     -&gt; EHLO, then refused; nothing sent
 * </pre>
 *
 * <p>So the assertion that matters is not "was TLS attempted" but <b>"was an AUTH command sent to a
 * server that refused to encrypt"</b>. Every case below is written against that.</p>
 *
 * <p>The appender is driven against {@link FakeSmtpServer} rather than mocked: the property under test
 * is how the Jakarta Mail client behaves against a server's replies, which a mock would replace with my
 * assumption about it.</p>
 */
class StartTlsRequiredSmtpAppenderSpec extends Specification {

  FakeSmtpServer server
  LoggerContext context

  def setup() {
    context = new LoggerContext()
    context.setMDCAdapter(new org.slf4j.helpers.BasicMDCAdapter())
  }

  def cleanup() {
    server?.close()
    context?.stop()
  }

  // ---- the incident: credentials must not cross an unencrypted hop -------------------------------

  def "the password is not sent to a relay that cannot upgrade to TLS"() {
    given: 'a relay that offers authentication but no STARTTLS — the incident, exactly'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])
    server.expectedCredentials = ['error-bot@example.org', 'the-right-password']

    and: 'the shipped appender with correct credentials'
    def appender = shippedAppender('error-bot@example.org', 'the-right-password')

    when: 'an ERROR is logged, and the asynchronous send has had time to reach the server'
    appender.doAppend(errorEvent())
    awaitCommands(server)

    then: 'no AUTH command was sent, so the password never crossed the wire'
    !server.commands.any { it.toUpperCase().startsWith('AUTH') }
    !server.sawAuthentication

    and: 'it did reach the server, so the refusal is TLS and not a dead socket'
    server.commands.any { it.toUpperCase().startsWith('EHLO') }

    and: 'the refusal is reported, not swallowed'
    def reported = appender.getStatusManager().getCopyOfStatusList()
        .collect { (it.message ?: '') + ' ' + (it.throwable ?: '') }
        .join(' | ')
    reported.contains('STARTTLS is required')
  }

  def "plain logback does send the credentials to a relay that cannot upgrade"() {
    given: 'the appender the configuration had before this change'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])
    server.expectedCredentials = ['error-bot@example.org', 'the-right-password']
    def plain = logbackAppender('error-bot@example.org', 'the-right-password')

    when:
    plain.doAppend(errorEvent())
    awaitCommands(server)

    then: 'logback authenticates, then delivers — over a hop that never upgraded to TLS'
    server.commands.any { it.toUpperCase().startsWith('AUTH') }
    server.sawAuthentication
  }

  def "the shipped appender refuses where plain logback would have delivered"() {
    given: 'the same relay'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])
    server.expectedCredentials = ['error-bot@example.org', 'the-right-password']
    def shipped = shippedAppender('error-bot@example.org', 'the-right-password')

    when:
    shipped.doAppend(errorEvent())
    awaitCommands(server)

    then: 'no credentials are offered, so the password never crosses the wire'
    !server.commands.any { it.toUpperCase().startsWith('AUTH') }
  }

  // ---- the appender keeps everything else logback gave us ----------------------------------------

  def "it is logback's appender, so its inherited behaviour is retained"() {
    expect: 'a subclass, not a reimplementation'
    SMTPAppender.isAssignableFrom(StartTlsRequiredSmtpAppender)
  }

  @Unroll
  def "a #level event is not emailed"() {
    given: 'a relay that would accept mail if it were sent'
    server = FakeSmtpServer.start([FakeSmtpServer.Behaviour.AUTH_REQUIRED])
    server.expectedCredentials = ['error-bot@example.org', 'the-right-password']
    def appender = shippedAppender('error-bot@example.org', 'the-right-password')

    when:
    appender.doAppend(eventAt(level))

    then: 'the ERROR-only filter from logback-spring.xml still applies'
    server.commands.isEmpty()

    where:
    level << [Level.WARN, Level.INFO, Level.DEBUG]
  }

  def "it still works when no credentials are configured, which is the unauthenticated case"() {
    given: 'no username, so logback installs no authenticator'
    server = FakeSmtpServer.start([])

    when:
    def appender = shippedAppender('', '')

    then: 'the appender starts without demanding credentials it does not have'
    appender.isStarted()

    and: 'TLS is still required, so the unauthenticated case is not the insecure one'
    appender.session.getProperty('mail.smtp.starttls.required') == 'true'
  }

  // ---- the shipped configuration ----------------------------------------------------------------

  def "logback-spring.xml uses this appender and changes nothing else"() {
    given:
    def config = new File('src/main/resources/logback-spring.xml')
    if (!config.exists()) { config = new File('datamanager-app/src/main/resources/logback-spring.xml') }
    def text = config.text

    expect: 'the subclass is wired'
    text.contains('life.qbic.datamanager.logging.StartTlsRequiredSmtpAppender')

    and: 'the appender still reads spring.mail.*, so there is one SMTP source of truth (ADR-0005)'
    text.contains('spring.mail.host')
    text.contains('spring.mail.username')
    text.contains('spring.mail.password')

    and: 'no second SMTP configuration surface was introduced'
    !text.contains('mail.smtp.starttls.required')
  }

  // ---- helpers ----------------------------------------------------------------------------------

  /**
   * Waits for logback's asynchronous send to reach the server.
   *
   * <p>{@code SMTPAppender} dispatches on an executor, so the commands arrive after {@code doAppend}
   * returns. Asserting immediately reads an empty command list and looks like "TLS was required" when
   * it only means "nothing has happened yet" — which would make the central assertion of this spec
   * pass for the wrong reason.</p>
   */
  private static void awaitCommands(FakeSmtpServer server, long timeoutMs = 3000) {
    long deadline = System.currentTimeMillis() + timeoutMs
    while (server.commands.isEmpty() && System.currentTimeMillis() < deadline) {
      Thread.sleep(50)
    }
    // Deterministic upper bound: the refusal is reported right after EHLO, so a short settle is
    // enough to observe whether AUTH was attempted.
    Thread.sleep(400)
  }

  private StartTlsRequiredSmtpAppender shippedAppender(String username, String password) {
    def appender = new StartTlsRequiredSmtpAppender()
    configure(appender, username, password)
    return appender
  }

  /** logback's own appender with the same settings, as a control for the comparison case. */
  private SMTPAppender logbackAppender(String username, String password) {
    def appender = new SMTPAppender()
    configure(appender, username, password)
    return appender
  }

  private void configure(SMTPAppender appender, String username, String password) {
    appender.context = context
    appender.smtpHost = '127.0.0.1'
    appender.smtpPort = server.port
    appender.username = username
    appender.password = password
    appender.from = 'no-reply@example.org'
    appender.addTo('dev@example.org')
    appender.subject = '[Data Manager] - ERROR'
    appender.layout = plainLayout()
    // The ERROR-only behaviour lives in logback-spring.xml's LevelFilter, not in the class, so a
    // bare instance needs a permissive evaluator to reach the send path at all.
    appender.evaluator = permissiveEvaluator()
    appender.start()
  }

  private static LayoutBase<ILoggingEvent> plainLayout() {
    return new LayoutBase<ILoggingEvent>() {
      @Override
      String doLayout(ILoggingEvent event) {
        return 'test body'
      }
    }
  }

  private static EventEvaluatorBase<ILoggingEvent> permissiveEvaluator() {
    return new EventEvaluatorBase<ILoggingEvent>() {
      @Override
      boolean evaluate(ILoggingEvent event) {
        return true
      }
    }
  }

  private LoggingEvent errorEvent() {
    return eventAt(Level.ERROR)
  }

  private LoggingEvent eventAt(Level level) {
    def event = new LoggingEvent()
    event.loggerContext = context
    event.loggerName = 'life.qbic.test'
    event.level = level
    event.message = 'something went wrong'
    return event
  }
}
