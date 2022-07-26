package life.qbic.newshandler.usermanagement.passwordreset;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>Password Reset Link Supplier</b>
 *
 * <p>Creates a actionable URL that represents a password reset entry point</p>
 *
 * @since 1.0.0
 */
@Service
public class PasswordResetLinkSupplier {

  private final String protocol;

  private final String host;

  private final int port;

  private final String resetEndpoint;

  private final String passwordResetParameter;

  public PasswordResetLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${password-reset-endpoint}") String resetEndpoint,
      @Value("${password-reset-parameter}") String passwordResetParameter) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.resetEndpoint = resetEndpoint;
    this.passwordResetParameter = passwordResetParameter;
  }

  public String passwordResetUrl(String userId) throws MalformedURLException {
    String pathWithQuery = "/" + resetEndpoint + "?" + passwordResetParameter + "=" + userId;

    return new URL(protocol, host, port, pathWithQuery).toExternalForm();
  }
}
