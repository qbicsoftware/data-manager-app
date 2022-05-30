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


  private final String host;

  private final String port;

  private final String emailConfirmationEndpoint;

  public EmailConfirmationLinkSupplier(@Value("${server.address}") String host,
      @Value("${server.port}") String port,
      @Value("${email-confirmation-endpoint}") String emailConfirmationEndpoint) {
    this.host = host;
    this.port = port;
    this.emailConfirmationEndpoint = emailConfirmationEndpoint;
  }


  public String emailConfirmationUrl(String userId) {
    String hostAddress = String.join(":", host, port);
    return String.join("/", hostAddress, emailConfirmationEndpoint, userId);
  }
}
