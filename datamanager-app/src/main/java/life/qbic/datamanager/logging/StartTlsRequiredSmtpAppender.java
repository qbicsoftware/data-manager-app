package life.qbic.datamanager.logging;

import ch.qos.logback.classic.net.SMTPAppender;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.util.Properties;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b>logback's {@link SMTPAppender}, with TLS <em>required</em> instead of merely enabled.</b>
 *
 * <p>This class exists to change exactly one thing. Everything else — buffering, the event evaluator,
 * the layout, the ERROR-only filter, asynchronous sending, and the authenticated session — is
 * logback's, inherited unchanged.</p>
 *
 * <h3>Why subclassing is enough, and why a rewrite was not needed</h3>
 *
 * <p>An earlier attempt replaced the appender outright, on the belief that {@code SMTPAppender} does
 * not authenticate because it sends with the credential-free {@code Transport.send(Message)}. That
 * belief was wrong: {@code Transport} falls back to the {@link Authenticator} logback installs in the
 * session, and a deliberately wrong password produces a loud {@code 535 5.7.8} — verified against a
 * fake relay, not inferred. The rewrite was withdrawn.</p>
 *
 * <p>What is genuinely missing is a way to say <em>require</em> TLS. logback offers
 * {@code <STARTTLS>true</STARTTLS>}, which sets {@code mail.smtp.starttls.enable} — and
 * <b>{@code enable} falls back to cleartext</b> when the server does not offer STARTTLS. There is no
 * {@code required} option. Measured against a relay that cannot upgrade, with correct credentials:</p>
 *
 * <pre>
 * no STARTTLS configured        -&gt; AUTH LOGIN &lt;user&gt; &lt;pass&gt; ... mail delivered   (cleartext)
 * &lt;STARTTLS&gt;true&lt;/STARTTLS&gt;   -&gt; AUTH LOGIN &lt;user&gt; &lt;pass&gt; ... mail delivered   (cleartext)
 * starttls.required             -&gt; EHLO, then refused: "STARTTLS is required but host does not
 *                                  support STARTTLS"                                (nothing sent)
 * </pre>
 *
 * <p>So "just add {@code <STARTTLS>}true}" would <em>not</em> have changed the outcome of the
 * incident. The mail still leaves with the password on the wire; only the {@code Received} header would
 * look reassuring.</p>
 *
 * <p>{@link SMTPAppenderBase#session} is {@code protected} and {@link SMTPAppenderBase#start()} is
 * overridable, which makes the fix a few lines: let logback build its session and its {@link
 * Authenticator} exactly as it always does, then hand it a copy of the properties with {@code
 * mail.smtp.starttls.required} added.</p>
 *
 * @since 1.20.4
 * @see <a href="../configuration/MailConnectionCheck.html">MailConnectionCheck</a>, which applies the
 *     same requirement to the application's own mail path
 */
public class StartTlsRequiredSmtpAppender extends SMTPAppender {

  private static final Logger log = LoggerFactory.logger(StartTlsRequiredSmtpAppender.class);

  /**
   * Strengthens the session logback built, then leaves everything else to the parent.
   *
   * <p>Called once, at configuration time. The replacement {@link Session} keeps every property
   * logback set — including {@code mail.smtp.auth} and the host/port — and adds only the requirement.
   * The authenticator is reconstructed because {@link Session#getInstance} takes it by argument and
   * logback does not expose the one it created; it carries the same credentials the parent was
   * configured with, so authentication behaves identically.</p>
   */
  @Override
  public void start() {
    super.start();
    if (this.session == null) {
      // The parent refused to start (missing host, layout or evaluator). Do not paper over it.
      return;
    }

    Properties properties = new Properties();
    properties.putAll(this.session.getProperties());
    if (properties.getProperty("mail.smtp.host") == null) {
      String host = getSmtpHost();
      if (host != null) {
        properties.setProperty("mail.smtp.host", host);
      }
    }
    properties.setProperty("mail.smtp.starttls.required", "true");
    // 'enable' too: 'required' alone makes Jakarta Mail demand the upgrade, but some servers only
    // advertise it once the client has said it can do it.
    properties.setProperty("mail.smtp.starttls.enable", "true");

    this.session = Session.getInstance(properties, authenticatorForCurrentConfiguration());

    if (Boolean.parseBoolean(properties.getProperty("mail.smtp.auth"))) {
      log.info("Error-notification mail requires STARTTLS; a server that cannot upgrade will be "
          + "reported as a send failure rather than sent in cleartext.");
    } else {
      log.debug("Error-notification mail has no credentials configured; TLS is required but the "
          + "connection will be unauthenticated.");
    }
  }

  /**
   * Rebuilds the authenticator logback would have created.
   *
   * <p>Only when a username is configured — that is the same condition
   * {@code SMTPAppenderBase#buildSessionFromProperties} uses to decide whether to set {@code
   * mail.smtp.auth} and install an authenticator. Mirroring it keeps this class from changing
   * authentication behaviour, which is not what it is for.</p>
   */
  private Authenticator authenticatorForCurrentConfiguration() {
    String username = getUsername();
    if (username == null || username.trim().isEmpty()) {
      return null;
    }
    String password = getPassword();
    return new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    };
  }
}
