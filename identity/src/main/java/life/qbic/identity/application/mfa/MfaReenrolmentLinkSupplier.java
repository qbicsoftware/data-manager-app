package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>Builds the absolute URL of the second-factor enrolment link</b>
 *
 * <p>Mirrors {@code PasswordResetLinkSupplier}: same host properties, same construction, one different
 * endpoint. The absolute form is required because the link is read in a mail client, outside the
 * application, so a context-relative path would not resolve — and this deployment runs under a context
 * path ({@code /dev}), which is exactly the case a hand-built {@code /login/...} gets wrong.</p>
 *
 * <p>The token is a credential for the length of its life: it must never be logged, and it appears here
 * only as it is placed into the link (AUTH-R-05).</p>
 *
 * @since 1.0.0
 */
@Service
public class MfaReenrolmentLinkSupplier {

  private final String protocol;
  private final String host;
  private final int port;
  private final String contextPath;
  private final String endpoint;
  private final String tokenParameter;

  public MfaReenrolmentLinkSupplier(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${server.servlet.context-path}") String contextPath,
      @Value("${routing.mfa-reenrol.endpoint:/login/re-enrol}") String endpoint,
      @Value("${routing.mfa-reenrol.token-parameter:token}") String tokenParameter) {
    this.protocol = requireNonNull(protocol, "protocol must not be null");
    this.host = requireNonNull(host, "host must not be null");
    this.port = port;
    this.contextPath = contextPath == null ? "" : contextPath;
    this.endpoint = requireNonNull(endpoint, "endpoint must not be null");
    this.tokenParameter = requireNonNull(tokenParameter, "tokenParameter must not be null");
  }

  /**
   * @param rawToken the raw single-use token; the caller must not persist or log it
   * @return the absolute enrolment URL to put in the mail
   */
  public String reenrolmentUrl(String rawToken) {
    String pathWithQuery = contextPath + endpoint + "?" + tokenParameter + "=" + rawToken;
    try {
      return new URL(protocol, host, port, pathWithQuery).toExternalForm();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Cannot create the second-factor enrolment link.", e);
    }
  }
}
