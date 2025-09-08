package life.qbic.projectmanagement.infrastructure.ontology;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ontology.LookupException;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.TerminologySelect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("development")
public class MockTerminologySelect implements TerminologySelect {

  private static final Logger log = logger(MockTerminologySelect.class);

  private static void logWarning() {
    log.warn("Using mock implementation. Not suited for production deployment.");
  }

  @Override
  public List<OntologyClass> query(String searchTerm, int offset, int limit)
      throws LookupException {
    logWarning();
    return List.of();
  }

  @Override
  public Optional<OntologyClass> searchByCurie(String curie) throws LookupException {
    logWarning();
    return Optional.empty();
  }

  @Override
  public List<OntologyClass> search(String searchTerm, int offset, int limit)
      throws LookupException {
    logWarning();
    return List.of();
  }
}
