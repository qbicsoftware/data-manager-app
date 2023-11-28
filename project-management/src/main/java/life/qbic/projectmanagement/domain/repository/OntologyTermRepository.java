package life.qbic.projectmanagement.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.OntologyClassEntity;


/**
 * <b>Ontology Term Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link OntologyClassEntity} data storage.
 *
 * @since 1.0.0
 */
public interface OntologyTermRepository {

  /**
   * Searches for ontology terms that contain the provided name
   *
   * @param name the name to search for
   * @return entities that contain the name
   * @since 1.0.0
   */
  List<OntologyClassEntity> find(String name);

  Optional<OntologyClassEntity> find(Long ontologyClassId);

  /**
   * Thrown when a term is expected to exist but cannot be found.
   */
  class TermNotFoundException extends RuntimeException {

    public TermNotFoundException() {
    }

    public TermNotFoundException(Throwable cause) {
      super(cause);
    }
  }
}
