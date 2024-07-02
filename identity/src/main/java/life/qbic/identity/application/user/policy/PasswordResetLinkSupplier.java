package life.qbic.identity.application.user.policy;

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

    private final String contextPath;

    private final String resetEndpoint;

    private final String passwordResetParameter;

    public PasswordResetLinkSupplier(
            @Value("${service.host.protocol}") String protocol,
            @Value("${service.host.name}") String host,
            @Value("${service.host.port}") int port,
            @Value("${server.servlet.context-path}") String contextPath,
        @Value("${routing.password-reset.endpoint}") String resetEndpoint,
        @Value("${routing.password-reset.reset-parameter}") String passwordResetParameter) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.contextPath = contextPath;
        this.resetEndpoint = resetEndpoint;
        this.passwordResetParameter = passwordResetParameter;
    }

    public String passwordResetUrl(String userId) {
        String pathWithQuery = contextPath + resetEndpoint + "?" + passwordResetParameter + "=" + userId;
        try {
            URL url = new URL(protocol, host, port, pathWithQuery);
            return url.toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot create password reset link.", e);
        }
    }
}
