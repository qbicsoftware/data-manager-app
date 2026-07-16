package life.qbic.projectmanagement.infrastructure.external.invenio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.associated_dataset.SourceInstanceDescriptor;
import life.qbic.projectmanagement.application.associated_dataset.SourceInstanceRegistry;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

/**
 * {@link SourceInstanceRegistry} backed by Spring-injected
 * {@link InvenioRdmProperties}.
 *
 * <p>The instance list is loaded once at application startup from
 * {@code application.properties} (see {@link InvenioRdmProperties}).
 * Adding a new instance requires a config change + redeploy (ADR-0002
 * I2); users cannot add arbitrary URLs via the UI.</p>
 *
 * @since 1.12.0
 */
public class PropertiesBackedSourceInstanceRegistry implements SourceInstanceRegistry {

  private static final Logger log = LoggerFactory.logger(
      PropertiesBackedSourceInstanceRegistry.class);

  private final List<SourceInstanceDescriptor> descriptors;

  public PropertiesBackedSourceInstanceRegistry(InvenioRdmProperties properties) {
    Objects.requireNonNull(properties, "properties must not be null");
    this.descriptors = properties.getInstances().stream()
        .filter(e -> e.getId() != null && e.getBaseUrl() != null)
        .map(e -> new SourceInstanceDescriptor(
            e.getId(),
            e.getDisplayName() != null ? e.getDisplayName() : e.getId(),
            e.getBaseUrl()))
        .toList();
    log.info("Loaded %d InvenioRDM instance(s): %s"
        .formatted(descriptors.size(),
            descriptors.stream().map(SourceInstanceDescriptor::id).toList()));
  }

  @Override
  public Optional<SourceInstanceDescriptor> find(String instanceId) {
    Objects.requireNonNull(instanceId, "instanceId must not be null");
    return descriptors.stream()
        .filter(d -> d.id().equals(instanceId))
        .findFirst();
  }

  @Override
  public List<SourceInstanceDescriptor> findBySourceType(SourceType sourceType) {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    if (sourceType != SourceType.INVENIO_RDM) {
      return List.of();
    }
    return descriptors;
  }
}
