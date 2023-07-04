package life.qbic.authentication.application.user.policy;

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

  private final String proxyPath;

  private final String resetEndpoint;

  private final String passwordResetParameter;

  public PasswordResetLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${service.host.proxy-path}") String proxyPath,
      @Value("${password-reset-endpoint}") String resetEndpoint,
      @Value("${password-reset-parameter}") String passwordResetParameter) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.proxyPath = proxyPath;
    this.resetEndpoint = resetEndpoint;
    this.passwordResetParameter = passwordResetParameter;
  }

  public String passwordResetUrl(String userId) {
    String pathWithQuery = "/" + resetEndpoint + "?" + passwordResetParameter + "=" + userId;
    try {
      URL url = proxyPath.isBlank() ? urlWithoutProxy(protocol, host, port, pathWithQuery) : urlWithProxy(protocol, host, port, proxyPath, pathWithQuery);
      return url.toExternalForm();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Cannot create password reset link.", e);
    }
  }

  private URL urlWithProxy(String protocol, String host, int port, String proxyPath, String actionPath) throws MalformedURLException {
    return new URL(protocol, host, port, "/" + proxyPath + actionPath);
  }

  private URL urlWithoutProxy(String protocol, String host, int port, String actionPath) throws MalformedURLException {
    return new URL(protocol, host, port, actionPath);
  }
}
