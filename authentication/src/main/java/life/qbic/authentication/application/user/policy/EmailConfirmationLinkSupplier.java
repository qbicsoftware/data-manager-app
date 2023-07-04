package life.qbic.authentication.application.user.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Supplies mail confirmation routes
 *
 * @since 1.0.0
 */
@Service
public class EmailConfirmationLinkSupplier {

    private final String protocol;

    private final String host;

    private final int port;

    private final String proxyPath;

    private final String emailConfirmationEndpoint;

    private final String emailConfirmationParameter;

    public EmailConfirmationLinkSupplier(
            @Value("${service.host.protocol}") String protocol,
            @Value("${service.host.name}") String host,
            @Value("${service.host.port}") int port,
            @Value("${service.host.proxy-path}") String proxyPath,
            @Value("${email-confirmation-endpoint}") String loginEndpoint,
            @Value("${email-confirmation-parameter}") String emailConfirmationParameter) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.proxyPath = proxyPath;
        this.emailConfirmationEndpoint = loginEndpoint;
        this.emailConfirmationParameter = emailConfirmationParameter;
    }

    public String emailConfirmationUrl(String userId) {
        String pathWithQuery =
                "/" + emailConfirmationEndpoint + "?" + emailConfirmationParameter + "=" + userId;
        try {
            URL url = proxyPath.isBlank() ? urlWithoutProxy(protocol, host, port, pathWithQuery) : urlWithProxy(protocol, host, port, proxyPath, pathWithQuery);
            return url.toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Link creation failed.", e);
        }

    }

    private URL urlWithProxy(String protocol, String host, int port, String proxyPath, String actionPath) throws MalformedURLException {
        return new URL(protocol, host, port, "/" + proxyPath + actionPath);
    }

    private URL urlWithoutProxy(String protocol, String host, int port, String actionPath) throws MalformedURLException {
        return new URL(protocol, host, port, actionPath);
    }
}
