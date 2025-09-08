package life.qbic.projectmanagement.infrastructure.organisations;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.OrganisationRepository;
import life.qbic.projectmanagement.domain.Organisation;
import org.springframework.context.annotation.Profile;

@Profile("development")
public class MockOrganisationRepository implements OrganisationRepository {

  private static final Logger log = logger(MockOrganisationRepository.class);

  private static void logWarning() {
    log.warn("Using mock implementation. Not suited for production deployment.");
  }

  @Override
  public Optional<Organisation> resolve(String iri) {
    logWarning();
    if (iri.equals("https://ror.org/00v34f693")) {
      return Optional.of(new Organisation(iri, "Quantitative Biology Center"));
    }
    return Optional.empty();
  }
}
