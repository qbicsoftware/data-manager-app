package life.qbic.identity.application.user.policy;

import java.net.MalformedURLException;
import java.net.URL;
import life.qbic.application.commons.ApplicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Supplies confirmation routes.
 *
 * <p>Builds {@code /registration/confirm?token=<raw>} (AUTH-R-05). The raw token is the only
 * credential in the link — it is not derived from the user's persistent identifier, so a link
 * discloses nothing about the account and cannot be reconstructed by anyone who learns the
 * identifier.</p>
 *
 * @since 1.0.0
 */
@Service
public class EmailConfirmationLinkSupplier {

  private final String protocol;
  private final String host;
  private final int port;
  private final String contextPath;
  private final String emailConfirmationEndpoint;
  private final String tokenParameter;

  public EmailConfirmationLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${server.servlet.context-path}") String contextPath,
      @Value("${routing.email-confirmation.endpoint}") String emailConfirmationEndpoint,
      @Value("${routing.email-confirmation.token-parameter:token}") String tokenParameter) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.contextPath = contextPath;
    this.emailConfirmationEndpoint = emailConfirmationEndpoint;
    this.tokenParameter = tokenParameter;
  }

  /**
   * @param rawToken the raw verification token; the caller must not persist it
   * @return the absolute confirmation URL
   */
  public String emailConfirmationUrl(String rawToken) {
    String pathWithQuery =
        contextPath + emailConfirmationEndpoint + "?" + tokenParameter + "=" + rawToken;
    try {
      return new URL(protocol, host, port, pathWithQuery).toExternalForm();
    } catch (MalformedURLException e) {
      throw new ApplicationException("Link creation failed.", e);
    }
  }
}
