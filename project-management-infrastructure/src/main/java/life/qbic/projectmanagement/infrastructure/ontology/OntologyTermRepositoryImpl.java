package life.qbic.projectmanagement.infrastructure.ontology;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.domain.repository.OntologyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * <b>Ontology term repository implementation</b>
 *
 * <p>Implementation for the {@link OntologyRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link OntologyClass} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicOntologyTermRepo}, which is injected as
 * dependency upon creation.
 * <p>
 *
 * @since 1.0.0
 */
@Service
public class OntologyTermRepositoryImpl implements OntologyRepository {

  private static final Logger log = logger(OntologyTermRepositoryImpl.class);
  private final QbicOntologyTermRepo ontologyTermRepo;

  @Autowired
  public OntologyTermRepositoryImpl(QbicOntologyTermRepo ontologyTermRepo) {
    this.ontologyTermRepo = ontologyTermRepo;
  }

  @Override
  public List<OntologyClass> find(String name) {
    return ontologyTermRepo.findOntologyClassEntitiesByClassName(name);
  }

  @Override
  public Optional<OntologyClass> find(Long id) {
    return ontologyTermRepo.findById(id);
  }

  @Override
  public Optional<OntologyClass> findByCuri(String curi) {
    return ontologyTermRepo.findOntologyClassEntitiesByClassName(curi).stream().findAny();
  }


}
