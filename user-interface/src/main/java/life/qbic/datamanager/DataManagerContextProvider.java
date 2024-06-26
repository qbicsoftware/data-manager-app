package life.qbic.datamanager;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import life.qbic.application.commons.ApplicationException;
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
  private final String samplesEndpoint;

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
      baseUrlApplication = new URL(protocol, host, port, contextPath);
    } catch (MalformedURLException e) {
      throw new ApplicationException("Initialization of context provider failed.", e);
    }
  }

  @Override
  public String urlToProject(String projectId) {
    try {
      return new URL(baseUrlApplication,
          Paths.get(baseUrlApplication.getPath(), projectInfoEndpoint.formatted(projectId))
              .toString()).toExternalForm();
    } catch (MalformedURLException e) {
      throw new ApplicationException("Data Manager context creation failed.", e);
    }
  }

  @Override
  public String urlToSamplePage(String projectId) {
    try {
      return new URL(baseUrlApplication,
          Paths.get(baseUrlApplication.getPath(), samplesEndpoint.formatted(projectId))
              .toString()).toExternalForm();
    } catch (MalformedURLException e) {
      throw new ApplicationException("Data Manager context creation failed.", e);
    }
  }
}
