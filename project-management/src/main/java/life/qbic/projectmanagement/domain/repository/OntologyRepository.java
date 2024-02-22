package life.qbic.projectmanagement.domain.repository;

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
