package life.qbic.newshandler.usermanagement.email;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b>Mail server configuration</b>
 *
 * <p>Sets up the SMTP mail server connection and provides MIME messages that can
 * be requested for email submission</p>
 *
 * @since 1.0.0
 */
public class MailServerConfiguration {

  private static final Logger log = LoggerFactory.logger(
      EmailSubmissionService.class);

  private static final boolean STARTTLS_ENABLED = true;

  private static final boolean SMTP_AUTH_ENABLED = true;

  private final Session session;

  public MailServerConfiguration(String smtpHost, int smtpPort, String smtpUser,
      String smtpPassword) {
    Properties props = new Properties();
    props.put("mail.smtp.auth", SMTP_AUTH_ENABLED ? "true" : "false");
    props.put("mail.smtp.starttls.enable", STARTTLS_ENABLED ? "true" : "false");
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", smtpPort);
    session = Session.getInstance(props, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(smtpUser, smtpPassword);
      }
    });
  }

  /**
   * Request a ready to send {@link MimeMessage}.
   *
   * @return
   * @since 1.0.0
   */
  public MimeMessage mimeMessage() {
    return new MimeMessage(session);
  }

}
