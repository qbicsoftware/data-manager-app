package life.qbic.projectmanagement.infrastructure.ontology;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.domain.repository.OntologyTermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * <b>Ontology term repository implementation</b>
 *
 * <p>Implementation for the {@link OntologyTermRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link OntologyClassEntity} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicOntologyTermRepo}, which is injected as
 * dependency upon creation.
 * <p>
 *
 * @since 1.0.0
 */
@Service
public class OntologyTermRepositoryImpl implements OntologyTermRepository {

  private static final Logger log = logger(OntologyTermRepositoryImpl.class);
  private final QbicOntologyTermRepo ontologyTermRepo;

  @Autowired
  public OntologyTermRepositoryImpl(QbicOntologyTermRepo ontologyTermRepo) {
    this.ontologyTermRepo = ontologyTermRepo;
  }

  @Override
  public List<OntologyClassEntity> find(String name) {
    return ontologyTermRepo.findOntologyClassEntitiesByClassName(name);
  }

  @Override
  public Optional<OntologyClassEntity> find(Long id) {
    return ontologyTermRepo.findById(id);
  }


}
