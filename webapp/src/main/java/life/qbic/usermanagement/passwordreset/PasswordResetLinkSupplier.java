package life.qbic.usermanagement.passwordreset;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;

/**
 * <b>Password Reset Link Supplier</b>
 *
 * <p>Creates a actionable URL that represents a password reset entry point</p>
 *
 * @since 1.0.0
 */
class PasswordResetLinkSupplier {

  private final String protocol;

  private final String host;

  private int port;

  private final String resetEndpoint;

  private final String passwordResetParameter;

  public PasswordResetLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${server.port}") int port,
      @Value("${password-reset-endpoint}") String resetEndpoint,
      @Value("${email-password-reset-parameter}") String passwordResetParameter) {
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
