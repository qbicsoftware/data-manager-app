package life.qbic.datamanager;

import java.net.MalformedURLException;
import java.net.URL;
import life.qbic.projectmanagement.application.AppContextProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>Data Manager context provider</b>
 * <p>
 * Simple implementation of the {@link AppContextProvider} interface.
 *
 * @since 1.0.0
 */
@Component
public class DataManagerContextProvider implements AppContextProvider {

  private final String protocol;
  private final String host;
  private final int port;
  private final String context;
  private final String endpoint;

  public DataManagerContextProvider(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${server.servlet.context-path}") String contextPath,
      @Value("${project-endpoint}") String projectEndpoint) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.context = contextPath;
    this.endpoint = projectEndpoint;
  }

  @Override
  public String urlToProject(String projectId) {
    var fullPath = basePathForProject(projectId) + "/info";
    return formURL(fullPath);
  }

  private String basePathForProject(String projectId) {
    return context + endpoint + "/" + projectId;
  }

  private String formURL(String path) {
    try {
      return new URL(protocol, host, port, path).toExternalForm();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String urlToSamplePage(String projectId) {
    var fullPath = basePathForProject(projectId) + "/samples";
    return formURL(fullPath);
  }
}
