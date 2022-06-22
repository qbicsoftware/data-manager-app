package life.qbic.usermanagement.passwordreset;

import org.springframework.beans.factory.annotation.Value;

/**
 * <b>Password Reset Link Supplier</b>
 *
 * <p>Creates a actionable URL that represents a password reset entry point</p>
 *
 * @since 1.0.0
 */
class PasswordResetLinkSupplier {

  private final String host;

  private final String port;

  private final String loginEndpoint;

  private final String emailConfirmationParameter;

  public PasswordResetLinkSupplier(@Value("${host.name}") String host,
      @Value("${server.port}") String port,
      @Value("${login-endpoint}") String loginEndpoint,
      @Value("${email-password-reset-parameter}") String emailConfirmationParameter) {
    this.host = host;
    this.port = port;
    this.loginEndpoint = loginEndpoint;
    this.emailConfirmationParameter = emailConfirmationParameter;
  }

  public String passwordResetUrl(String userId) {
    String hostAddress = String.join(":", host, port);
    String params = String.join("=", emailConfirmationParameter, userId);
    String endpointWithParams = String.join("?", loginEndpoint, params);
    return String.join("/", hostAddress, endpointWithParams);
  }
}
