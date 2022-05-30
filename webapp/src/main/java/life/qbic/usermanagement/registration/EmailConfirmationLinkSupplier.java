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

  private final String loginEndpoint;

  private final String emailConfirmationParameter;

  public EmailConfirmationLinkSupplier(@Value("${server.address}") String host,
      @Value("${server.port}") String port,
      @Value("${login-endpoint}") String loginEndpoint,
      @Value("${email-confirmation-parameter}") String emailConfirmationParameter) {
    this.host = host;
    this.port = port;
    this.loginEndpoint = loginEndpoint;
    this.emailConfirmationParameter = emailConfirmationParameter;
  }

  public String emailConfirmationUrl(String userId) {
    String hostAddress = String.join(":", host, port);
    String params = String.join("=", emailConfirmationParameter,  userId);
    String endpointWithParams = String.join("?", loginEndpoint, params);
    return String.join("/", hostAddress, endpointWithParams);
  }
}
