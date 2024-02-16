package life.qbic.projectmanagement.infrastructure.ontology;

import java.util.List;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import org.springframework.data.repository.CrudRepository;

/**
 * <b>QBiC ontology term interface</b>
 *
 * <p>This interface will be automatically detected by Spring on application startup and create an
 * instance of this class automatically.
 *
 * <p>Since it extends the {@link CrudRepository} class from Spring, no need to write queries. The
 * framework will do that for us.
 *
 * @since 1.0.0
 */
public interface QbicOntologyTermRepo extends CrudRepository<OntologyClass, Long> {

  List<OntologyClass> findOntologyClassEntitiesByClassName(String name);

}
