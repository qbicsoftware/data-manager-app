package life.qbic.usermanagement.registration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Supplies email confirmation routes
 *
 * @since 1.0.0
 */
@Service
public class EmailConfirmationLinkSupplier {

  @Value("${server.address}")
  private String host;
  @Value("${server.port}")
  private String port;
  @Value("${email-confirmation-endpoint}")
  private String emailConfirmationEndpoint;

  public String emailConfirmationUrl(String userId) {
    String hostAddress = String.join(":", host, port);
    return String.join("/", hostAddress, emailConfirmationEndpoint, userId);
  }
}
