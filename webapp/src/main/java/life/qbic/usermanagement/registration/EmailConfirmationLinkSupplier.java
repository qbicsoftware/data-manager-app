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

  private final String protocol;

  private final String host;

  private final String port;

  private final String loginEndpoint;

  private final String emailConfirmationParameter;

  public EmailConfirmationLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${server.port}") String port,
      @Value("${email-confirmation-endpoint}") String loginEndpoint,
      @Value("${email-confirmation-parameter}") String emailConfirmationParameter) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.loginEndpoint = loginEndpoint;
    this.emailConfirmationParameter = emailConfirmationParameter;
  }

  public String emailConfirmationUrl(String userId) {
    return String.format("%s://%s:%s/%s?%s=%s", protocol, host, port, loginEndpoint,
        emailConfirmationParameter, userId);
  }
}
