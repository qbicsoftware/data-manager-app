package life.qbic.datamanager;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.application.AppContextProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <b>Data Manager context provider</b>
 * <p>
 * Simple implementation of the {@link AppContextProvider} interface.
 *
 * @since 1.0.0
 */
@Component
public class DataManagerContextProvider implements AppContextProvider {

  private final String projectInfoEndpoint;

  private final URL baseUrlApplication;
  private final String samplesEndpoint;

  /**
   * Creates a context provider for the Data Manager application.
   * <p>
   * The {@code port} must be -1 when no port is required, in which case it is treated as "no
   * port" and omitted automatically when constructing links.
   */
  public DataManagerContextProvider(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${server.servlet.context-path}") String contextPath,
      @Value("${routing.projects.info.endpoint}") String projectEndpoint,
      @Value("${routing.projects.samples.enpoint}") String samplesEndpoint) {
    this.projectInfoEndpoint = projectEndpoint;
    this.samplesEndpoint = samplesEndpoint;
    try {
      // The base path must end in a slash so that relative endpoint paths are resolved
      // relative to it and keep the context path prefix. The context path is normalized to
      // a single leading and trailing slash regardless of how it is configured.
      String normalized = contextPath.replaceAll("^/+", "").replaceAll("/+$", "");
      String basePath = normalized.isEmpty() ? "/" : "/" + normalized + "/";
      URI baseUri = UriComponentsBuilder.newInstance()
          .scheme(protocol)
          .host(host)
          .port(port)
          .path(basePath)
          .build()
          .toUri();
      baseUrlApplication = baseUri.toURL();
    } catch (MalformedURLException e) {
      throw new ApplicationException("Initialization of context provider failed.", e);
    }
  }

  @Override
  public String urlToProject(String projectId) {
    try {
      return baseUrlApplication.toURI()
          .resolve(stripLeadingSlashes(projectInfoEndpoint.formatted(projectId)))
          .toURL()
          .toExternalForm();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new ApplicationException("Data Manager context creation failed.", e);
    }
  }

  @Override
  public String urlToSamplePage(String projectId, String experimentId) {
    try {
      return baseUrlApplication.toURI()
          .resolve(stripLeadingSlashes(samplesEndpoint.formatted(projectId, experimentId)))
          .toURL()
          .toExternalForm();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new ApplicationException("Data Manager context creation failed.", e);
    }
  }

  private static String stripLeadingSlashes(String path) {
    int start = 0;
    while (start < path.length() && path.charAt(start) == '/') {
      start++;
    }
    return path.substring(start);
  }
}
