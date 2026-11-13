package life.qbic.datamanager.configuration;

import jakarta.mail.MessagingException;
import java.time.Duration;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * <b>Refuse to start when mail submission credentials are rejected.</b>
 *
 * <p>This exists because of a delivered message, not a failing test. A confirmation email left a test
 * deployment with this hop in its headers:</p>
 *
 * <pre>
 * Received: from 192.168.178.48 (unknown [134.2.250.7])
 *     by mx03.uni-tuebingen.de (Postfix) with ESMTP id A718220C4846
 * </pre>
 *
 * <p>{@code ESMTP} rather than {@code ESMTPS}: unencrypted. An external recipient: relayed. And the
 * configured mail password was stale, so <em>no authentication succeeded</em>. The application
 * reported success throughout — the confirmation mail "sent", the token leaked in cleartext over an
 * unauthenticated hop, and nothing anywhere said so.</p>
 *
 * <p><b>Why a startup check and not a send-time one.</b> {@code JavaMailSenderImpl} does throw on a
 * rejected login, but only when a login is attempted, and Jakarta Mail only <em>offers</em>
 * credentials. A relay that does not require authentication accepts the message anyway. So the
 * failure is not "the send threw" — it is "the send succeeded, insecurely", which is indistinguishable
 * from success at the call site. The only place left to notice it is at startup, where a wrong
 * password or a server that will not do TLS can be made fatal instead of silent.</p>
 *
 * <p><b>What it checks.</b> {@link JavaMailSenderImpl#testConnection()} connects through
 * {@code connectTransport()}, i.e. {@code Transport.connect(host, port, username, password)}, which
 * performs the SASL exchange and — given {@code mail.smtp.starttls.required} — fails if the server
 * will not upgrade to TLS. Both defects from the incident are therefore caught by one call.</p>
 *
 * <p><b>Why not {@code spring.mail.test-connection}.</b> Boot 4 provides exactly that
 * ({@code MailSenderValidatorAutoConfiguration}), and it would be one property instead of this class.
 * It is not used because it cannot be switched off per environment without editing the properties
 * file, and it has no timeout: a black-holed SMTP host would hang startup indefinitely rather than
 * fail. Local development and CI legitimately run without a reachable mail server, so an opt-out is
 * required; and a startup check that can hang is not a gate.</p>
 *
 * @since 1.20.4
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("qbic.mail.verify-connection-on-startup")
@ConditionalOnSingleCandidate(JavaMailSenderImpl.class)
public class MailConnectionCheck {

  /**
   * How long the check may take before it is treated as a failure.
   *
   * <p>Applied around {@code testConnection()} on a separate thread because the Jakarta Mail
   * connect has no timeout of its own that covers a black-holed host. An unreachable server is a
   * deployment fault and must surface as a failed start, not as an application that half-exists.</p>
   */
  private static final Duration TIMEOUT = Duration.ofSeconds(15);

  private static final Logger log = LoggerFactory.logger(MailConnectionCheck.class);

  public MailConnectionCheck(JavaMailSenderImpl mailSender) {
    String host = mailSender.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalStateException(
          "Mail connection check is enabled but spring.mail.host is not set. Either configure the "
              + "mail server or disable qbic.mail.verify-connection-on-startup.");
    }
    if (mailSender.getUsername() == null || mailSender.getUsername().isBlank()) {
      throw new IllegalStateException(
          "Mail connection check is enabled but spring.mail.username is empty. Mail would be "
              + "submitted unauthenticated; set MAIL_USERNAME or disable "
              + "qbic.mail.verify-connection-on-startup.");
    }

    log.info("Verifying SMTP connection to %s:%d".formatted(host, mailSender.getPort()));
    try {
      runWithTimeout(mailSender);
    } catch (MessagingException e) {
      throw new IllegalStateException(
          ("Mail server %s:%d rejected the configured credentials or refused an encrypted "
              + "connection. Mail must not be sent unauthenticated or in cleartext; fix the "
              + "credentials/TLS settings, or disable qbic.mail.verify-connection-on-startup.")
              .formatted(host, mailSender.getPort()), e);
    }
    log.info("SMTP connection to %s:%d verified".formatted(host, mailSender.getPort()));
  }

  /**
   * Runs the connection test with a hard timeout, so an unreachable host fails startup instead of
   * blocking it.
   *
   * <p>The worker is a virtual thread: the check runs once at startup and only blocks on I/O, which
   * is precisely what virtual threads are for, and it keeps the extra thread off the platform pool.</p>
   */
  private static void runWithTimeout(JavaMailSenderImpl mailSender) throws MessagingException {
    var failure = new java.util.concurrent.atomic.AtomicReference<MessagingException>();
    Thread worker = Thread.ofVirtual().name("mail-connection-check").start(() -> {
      try {
        mailSender.testConnection();
      } catch (MessagingException e) {
        failure.set(e);
      }
    });

    try {
      worker.join(TIMEOUT.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while verifying the SMTP connection", e);
    }

    if (worker.isAlive()) {
      worker.interrupt();
      throw new IllegalStateException(
          "Verifying the SMTP connection did not complete within %d seconds; the server is "
              .formatted(TIMEOUT.toSeconds())
              + "unreachable or not answering. Refusing to start.");
    }
    if (failure.get() != null) {
      throw failure.get();
    }
  }
}
