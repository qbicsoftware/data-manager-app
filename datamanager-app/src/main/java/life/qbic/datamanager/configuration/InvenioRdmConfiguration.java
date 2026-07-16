package life.qbic.datamanager.configuration;

import life.qbic.projectmanagement.application.associated_dataset.DatasetSource;
import life.qbic.projectmanagement.application.associated_dataset.SourceInstanceRegistry;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmClient;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmClient.InvenioRdmHttpClient;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmDatasetSource;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmProperties;
import life.qbic.projectmanagement.infrastructure.external.invenio.PropertiesBackedSourceInstanceRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the InvenioRDM integration beans.
 *
 * <p>Registers:
 * <ul>
 *   <li>{@link InvenioRdmProperties} — config-bound properties
 *       ({@code qbic.external-service.invenio-rdm.*})</li>
 *   <li>{@link InvenioRdmClient} — low-level HTTP client (stateless,
 *       shared singleton)</li>
 *   <li>{@link SourceInstanceRegistry} — admin-configured instance
 *       lookup (ADR-0002 I2)</li>
 *   <li>{@link DatasetSource} — the port adapter for InvenioRDM
 *       (ADR-0002 P2)</li>
 * </ul>
 *
 * @since 1.12.0
 */
@Configuration
@EnableConfigurationProperties(InvenioRdmProperties.class)
public class InvenioRdmConfiguration {

  @Bean
  public InvenioRdmClient invenioRdmClient() {
    return new InvenioRdmHttpClient();
  }

  @Bean
  public SourceInstanceRegistry sourceInstanceRegistry(InvenioRdmProperties properties) {
    return new PropertiesBackedSourceInstanceRegistry(properties);
  }

  @Bean
  public DatasetSource invenioRdmDatasetSource(InvenioRdmClient client) {
    return new InvenioRdmDatasetSource(client);
  }
}
