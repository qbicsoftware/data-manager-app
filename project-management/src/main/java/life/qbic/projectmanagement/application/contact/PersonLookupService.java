package life.qbic.projectmanagement.application.contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
   * Queries {@link OrcidEntry} with a provided offset
   * and limit that supports pagination.
   *
   * @param filter the user's input will be applied to filter results
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   */
  public List<OrcidEntry> queryPersons(String filter, int offset, int limit) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<OrcidEntry> orcidEntries = new ArrayList<>();
    //Orcid Repository will return an Error 500 if the search string starts with a special character
    // or contains one of the non-listed characters here (such as % & or :)
    String validRegex = "^[A-Za-z0-9][A-Za-z0-9.@_-]*$";
    //Return empty result if no query was provided by the user
    if (filter.isBlank()) {
      return orcidEntries;
    }
    //Return empty result if an invalid query was provided by the user
    if (!filter.matches(validRegex)) {
      return orcidEntries;
    }
    orcidEntries = personRepository.findAll(filter, limit, offset);
    // the list must be modifiable for spring security to filter it
    return orcidEntries;
  }
}
