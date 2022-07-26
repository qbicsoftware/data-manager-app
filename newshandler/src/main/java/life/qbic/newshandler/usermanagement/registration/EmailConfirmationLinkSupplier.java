package life.qbic.newshandler.usermanagement.registration;

import java.net.MalformedURLException;
import java.net.URL;
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

  private final int port;

  private final String emailConfirmationEndpoint;

  private final String emailConfirmationParameter;

  public EmailConfirmationLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${email-confirmation-endpoint}") String loginEndpoint,
      @Value("${email-confirmation-parameter}") String emailConfirmationParameter) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.emailConfirmationEndpoint = loginEndpoint;
    this.emailConfirmationParameter = emailConfirmationParameter;
  }

  public String emailConfirmationUrl(String userId) throws MalformedURLException {
    String pathWithQuery = "/" + emailConfirmationEndpoint + "?" + emailConfirmationParameter + "=" + userId;

    return new URL(protocol, host, port, pathWithQuery).toExternalForm();
  }
}
