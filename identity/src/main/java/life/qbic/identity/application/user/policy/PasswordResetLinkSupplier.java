package life.qbic.identity.application.user.policy;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>Password Reset Link Supplier</b>
 *
 * <p>Creates an actionable URL that represents a password reset entry point.</p>
 *
 * <p>The URL carries a single-use, time-limited token, never the user's persistent identifier
 * (AUTH-R-05). A reset link grants control of an existing account, so it is a credential in every
 * sense: it must not be derivable from anything an observer could already know.</p>
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

    private final String tokenParameter;

    public PasswordResetLinkSupplier(
            @Value("${service.host.protocol}") String protocol,
            @Value("${service.host.name}") String host,
            @Value("${service.host.port}") int port,
            @Value("${server.servlet.context-path}") String contextPath,
        @Value("${routing.password-reset.endpoint}") String resetEndpoint,
        @Value("${routing.password-reset.token-parameter:token}") String tokenParameter) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.contextPath = contextPath;
        this.resetEndpoint = resetEndpoint;
        this.tokenParameter = tokenParameter;
    }

    /**
     * @param rawToken the raw single-use reset token; the caller must not persist it
     * @return the absolute password reset URL
     */
    public String passwordResetUrl(String rawToken) {
        String pathWithQuery = contextPath + resetEndpoint + "?" + tokenParameter + "=" + rawToken;
        try {
            URL url = new URL(protocol, host, port, pathWithQuery);
            return url.toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot create password reset link.", e);
        }
    }
}
