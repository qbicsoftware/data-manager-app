package life.qbic.datamanager.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.Serial;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <b>Sends an email for every ERROR event — over an authenticated, TLS-required SMTP connection.</b>
 *
 * <p>Replaces logback's {@link ch.qos.logback.classic.net.SMTPAppender}, which could not be made to
 * authenticate. Its own source shows why:</p>
 *
 * <pre>
 * // SMTPAppenderBase#buildSessionFromProperties
 * loginAuthenticator = new LoginAuthenticator(username, password);
 * props.put(MAIL_SMTP_AUTH_PK, "true");
 * ...
 * return Session.getInstance(props, loginAuthenticator);
 *
 * // SMTPAppenderBase#sendBuffer
 * Transport.send(mimeMsg);          // no credentials argument
 * </pre>
 *
 * <p>The credentials are collected, put into an {@code Authenticator}, and then never used:
 * {@code Transport.send(Message)} resolves an address with <em>no</em> username, so {@code
 * Service.connect(service, user, password)} is called with nulls and the {@code Authenticator} is
 * never consulted. A relay that does not demand authentication therefore accepts the message, and a
 * <em>wrong password is silently ignored</em> — the mail leaves unauthenticated, and the only trace is
 * an {@code addError} buried in the logging subsystem.</p>
 *
 * <p>That is not a hypothetical. During the mail-submission investigation a deployment sent mail with
 * a stale password while the application reported success; logback's appender is the second channel
 * with that defect, and it bypasses {@link life.qbic.datamanager.configuration.MailConnectionCheck}
 * because it builds its own {@link Session} instead of using the {@code JavaMailSender} bean.</p>
 *
 * <h3>Deliberate differences from the appender it replaces</h3>
 *
 * <ul>
 *   <li><b>Credentials are passed to {@code Transport.connect},</b> which is the only way Jakarta Mail
 *       actually authenticates when the remote offers it.</li>
 *   <li><b>{@code starttls.required} is set</b>, so a server that cannot encrypt the hop is a failure
 *       rather than a silent cleartext send.</li>
 *   <li><b>Failure never propagates.</b> This runs on the logging thread; throwing from an appender
 *       would turn a logging call into an application fault. Errors go to {@link #addError} and to
 *       stderr, never upward.</li>
 *   <li><b>One mail in flight at a time.</b> An error storm must not open a connection per event; while
 *       a send is in progress further events are dropped and the drop is reported. Losing a duplicate
 *       error notification is acceptable; exhausting sockets is not.</li>
 * </ul>
 *
 * <p><b>Configured entirely from {@code logback-spring.xml}</b>, keeping ADR-0005's principle that
 * {@code spring.mail.*} is the single source of SMTP configuration: every value below is read through
 * {@code <springProperty>}.</p>
 *
 * @since 1.20.4
 */
public class AuthenticatedSmtpAppender extends AppenderBase<ILoggingEvent> {

  @Serial
  private static final long serialVersionUID = 4471091542405512028L;

  private String smtpHost;
  private int smtpPort = 587;
  private String username;
  private String password;
  private String from;
  private String to;
  private String subjectPrefix;
  private Layout<ILoggingEvent> layout;

  /**
   * Guards against an error storm opening one connection per event.
   *
   * <p>Deliberately drops rather than queues: the purpose of this channel is to make a human aware
   * that something is wrong, and the first notification does that. Queuing unboundedly under a
   * failing mail server would turn a logging problem into a memory problem.</p>
   */
  private final AtomicBoolean sending = new AtomicBoolean(false);

  @Override
  public void start() {
    // Preserve the ERROR-only contract the previous appender enforced with a LevelFilter. Doing it
    // here rather than in the XML keeps the guarantee with the class: a misconfiguration in
    // logback-spring.xml must not be able to turn this into a mail-per-WARN channel.
    ThresholdFilter errorsOnly = new ThresholdFilter();
    errorsOnly.setLevel("ERROR");
    errorsOnly.start();
    addFilter(errorsOnly);

    if (smtpHost == null || smtpHost.isBlank()) {
      // Not a configuration to fail the application over: the log file is still the primary record.
      addError("No smtpHost configured; the error-notification appender will not send mail.");
    }
    if (username == null || username.isBlank() || password == null || password.isEmpty()) {
      addError("No SMTP credentials configured; error notification mail would be sent "
          + "unauthenticated and is therefore disabled.");
    }
    super.start();
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (!isStarted()) {
      return;
    }
    if (smtpHost == null || smtpHost.isBlank()
        || username == null || username.isBlank()
        || password == null || password.isEmpty()
        || to == null || to.isBlank()) {
      // start() already reported the missing configuration once; do not repeat per event.
      return;
    }
    if (!sending.compareAndSet(false, true)) {
      addError("A previous error notification is still in flight; this event was not emailed.");
      return;
    }
    try {
      send(event);
    } catch (MessagingException e) {
      // Never propagate: this runs inside a logging call.
      addError("Could not send the error notification email.", e);
    } finally {
      sending.set(false);
    }
  }

  private void send(ILoggingEvent event) throws MessagingException {
    Session session = Session.getInstance(sessionProperties());
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, true));
    message.setSubject(subjectPrefix == null ? "ERROR" : subjectPrefix + " - ERROR", "UTF-8");
    message.setText(layout == null ? event.getFormattedMessage() : layout.doLayout(event), "UTF-8");
    message.setSentDate(new Date());

    // The credentials go to connect(), not into the Session. This is the whole point: an
    // Authenticator in the Session is only consulted when Transport decides it needs a password,
    // and Transport.send(Message) never decides that.
    try (Transport transport = session.getTransport("smtp")) {
      transport.connect(smtpHost, smtpPort, username, password);
      transport.sendMessage(message, message.getAllRecipients());
    }
  }

  private Properties sessionProperties() {
    Properties properties = new Properties();
    properties.setProperty("mail.smtp.host", smtpHost);
    properties.setProperty("mail.smtp.port", Integer.toString(smtpPort));
    // Required, not merely enabled: 'enable' falls back to cleartext when the server does not offer
    // STARTTLS, which is the failure this class exists to end.
    properties.setProperty("mail.smtp.starttls.enable", "true");
    properties.setProperty("mail.smtp.starttls.required", "true");
    properties.setProperty("mail.smtp.auth", "true");
    // Bound the damage a non-answering server can do to the logging thread.
    properties.setProperty("mail.smtp.connectiontimeout", "10000");
    properties.setProperty("mail.smtp.timeout", "10000");
    properties.setProperty("mail.smtp.writetimeout", "10000");
    return properties;
  }

  // ---- logback configuration --------------------------------------------------------------------

  public void setSmtpHost(String smtpHost) {
    this.smtpHost = smtpHost;
  }

  public void setSmtpPort(int smtpPort) {
    this.smtpPort = smtpPort;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public void setSubjectPrefix(String subjectPrefix) {
    this.subjectPrefix = subjectPrefix;
  }

  public void setLayout(Layout<ILoggingEvent> layout) {
    this.layout = layout;
  }
}
