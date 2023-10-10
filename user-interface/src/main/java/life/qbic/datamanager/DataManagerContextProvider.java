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
  private final String projectInfoEndpoint;

  private final URL baseUrlApplication;

  public DataManagerContextProvider(
      @Value("${service.host.protocol}") String protocol,
      @Value("${service.host.name}") String host,
      @Value("${service.host.port}") int port,
      @Value("${server.servlet.context-path}") String contextPath,
      @Value("${project-info-endpoint}") String projectEndpoint) {
    this.projectInfoEndpoint = projectEndpoint;
    try {
      baseUrlApplication = new URL(protocol, host, port, contextPath);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String urlToProject(String projectId) {
    try {
      return new URL(baseUrlApplication, projectInfoEndpoint.formatted(projectId)).toExternalForm();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
