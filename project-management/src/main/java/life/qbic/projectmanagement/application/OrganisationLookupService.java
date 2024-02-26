package life.qbic.projectmanagement.application;

import java.util.Optional;
import life.qbic.projectmanagement.domain.Organisation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class OrganisationLookupService {

  private final OrganisationRepository organisationRepository;

  @Autowired
  public OrganisationLookupService(OrganisationRepository organisationRepository) {
    this.organisationRepository = organisationRepository;
  }

  Optional<Organisation> organisation(String iri) {
    return organisationRepository.resolve(iri);
  }

}
