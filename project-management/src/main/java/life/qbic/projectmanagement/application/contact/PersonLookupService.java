package life.qbic.projectmanagement.application.contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Person Lookup Service
 * <p>
 * Service that provides an API to query and filter person information
 */
@Service
public class PersonLookupService {

  PersonRepository personRepository;

  public PersonLookupService(@Autowired PersonRepository personRepository) {
    this.personRepository = Objects.requireNonNull(personRepository);
  }

  /**
   * Queries {@link life.qbic.projectmanagement.domain.model.project.Contact} with a provided offset
   * and limit that supports pagination.
   *
   * @param filter the user's input will be applied to filter results
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   */
  public List<Contact> queryPersons(String filter, int offset, int limit) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<Contact> contacts = new ArrayList<>();
    if (filter.isBlank()) {
      return contacts;
    }
    contacts = personRepository.findAll(filter, limit, offset);
    // the list must be modifiable for spring security to filter it
    return contacts;
  }
}
