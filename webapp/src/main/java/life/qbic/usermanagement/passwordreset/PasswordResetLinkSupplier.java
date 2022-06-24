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

  private final String protocol;

  private final String host;

  private final String port;

  private final String resetEndpoint;

  private final String emailConfirmationParameter;

  public PasswordResetLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${server.port}") String port,
      @Value("${password-reset-endpoint}") String resetEndpoint,
      @Value("${email-password-reset-parameter}") String emailConfirmationParameter) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.resetEndpoint = resetEndpoint;
    this.emailConfirmationParameter = emailConfirmationParameter;
  }

  public String passwordResetUrl(String userId) {
    return String.format("%s://%s:%s/%s?%s=%s", protocol, host, port, resetEndpoint,
        emailConfirmationParameter, userId);
  }
}
