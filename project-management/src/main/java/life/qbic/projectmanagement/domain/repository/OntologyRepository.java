package life.qbic.projectmanagement.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.ontology.OntologyClass;


/**
 * <b>Ontology Term Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link OntologyClass} data storage.
 *
 * @since 1.0.0
 */
public interface OntologyRepository {

  /**
   * Searches for ontology terms that contain the provided name
   *
   * @param name the name to search for
   * @return entities that contain the name
   * @since 1.0.0
   */
  List<OntologyClass> find(String name);

  Optional<OntologyClass> find(Long ontologyClassId);

  Optional<OntologyClass> findByCuri(String curi);

  /**
   * Thrown when a term is expected to exist but cannot be found.
   */
  class OntologyNotFoundException extends RuntimeException {

    public OntologyNotFoundException() {
    }

    public OntologyNotFoundException(Throwable cause) {
      super(cause);
    }
  }
}
