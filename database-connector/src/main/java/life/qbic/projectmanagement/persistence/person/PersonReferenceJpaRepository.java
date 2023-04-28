package life.qbic.projectmanagement.persistence.person;

import java.util.List;
import life.qbic.persistence.OffsetBasedRequest;
import life.qbic.projectmanagement.application.api.PersonLookupService;
import life.qbic.projectmanagement.domain.project.PersonReference;
import org.springframework.stereotype.Component;

/**
 * Simple implementation of the {@link PersonLookupService} interface.
 *
 * @since 1.0.0
 */
@Component
public class PersonReferenceJpaRepository implements PersonLookupService {

  private final PersonRepo personRepo;

  public PersonReferenceJpaRepository(PersonRepo personRepo) {
    this.personRepo = personRepo;
  }

  @Override
  public List<PersonReference> find(String filter, int offset, int limit) {
    var personsResult = personRepo.findPersonByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        filter,
        filter, new OffsetBasedRequest(offset, limit));
    return personsResult.getContent().stream().map(person -> new PersonReference(
        person.referenceId(), person.fullName(), person.emailAddress())).toList();
  }
}
