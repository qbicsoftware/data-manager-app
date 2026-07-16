package life.qbic.projectmanagement.application.associated_dataset;

import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

import java.util.List;
import java.util.Optional;

/**
 * Registry of available external data source instances.
 *
 * <p>Admin-configured (ADR-0002 I2): the list of instances is loaded
 * from application configuration (e.g. {@code application.properties})
 * and is fixed at deploy time. Users cannot add arbitrary instance URLs
 * through the UI.</p>
 *
 * @since 1.12.0
 */
public interface SourceInstanceRegistry {

  /**
   * Finds a configured instance by its unique ID.
   *
   * @param instanceId the instance identifier (e.g. "zenodo", "fdat")
   * @return the descriptor, or empty if no instance with that ID is
   *         configured
   */
  Optional<SourceInstanceDescriptor> find(String instanceId);

  /**
   * Returns all configured instances for a given source type.
   *
   * @param sourceType the source system type (e.g.
   *                   {@link SourceType#INVENIO_RDM})
   * @return all configured instances of that source type; never null
   *         (may be empty)
   */
  List<SourceInstanceDescriptor> findBySourceType(SourceType sourceType);

}
