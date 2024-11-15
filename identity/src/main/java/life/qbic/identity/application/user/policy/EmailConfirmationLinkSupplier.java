package life.qbic.identity.application.user.policy;

import java.net.MalformedURLException;
import java.net.URL;
import life.qbic.application.commons.ApplicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private final String contextPath;

    private final String emailConfirmationEndpoint;

    private final String emailConfirmationParameter;

    public EmailConfirmationLinkSupplier(
            @Value("${service.host.protocol}") String protocol,
            @Value("${service.host.name}") String host,
            @Value("${service.host.port}") int port,
            @Value("${server.servlet.context-path}") String contextPath,
        @Value("${routing.email-confirmation.endpoint}") String loginEndpoint,
        @Value("${routing.email-confirmation.confirmation-parameter}") String emailConfirmationParameter) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.contextPath = contextPath;
        this.emailConfirmationEndpoint = loginEndpoint;
        this.emailConfirmationParameter = emailConfirmationParameter;
    }

    public String emailConfirmationUrl(String userId) {
        String pathWithQuery = contextPath + emailConfirmationEndpoint + "?" + emailConfirmationParameter + "=" + userId;
        try {
            URL url = new URL(protocol, host, port, pathWithQuery);
            return url.toExternalForm();
        } catch (MalformedURLException e) {
            throw new ApplicationException("Link creation failed.", e);
        }

    }
}
