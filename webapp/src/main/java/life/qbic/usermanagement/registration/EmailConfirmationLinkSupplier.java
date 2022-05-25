package life.qbic.usermanagement.registration;

import org.springframework.beans.factory.annotation.Value;

/**
 * Supplies email confirmation routes
 *
 * @since 1.0.0
 */
public class EmailConfirmationLinkSupplier {

  @Value("${server.host}")
  private String host;
  @Value("${server.port}")
  private String port;
  @Value("${email-confirmation-endpoint}")
  private String emailConfirmationRoute;

  public String emailConfirmationUrl(String userId) {
    String hostAddress = String.join(":", host, port);
    return String.join("/", hostAddress, emailConfirmationRoute, userId);
  }
}
