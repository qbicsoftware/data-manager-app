package life.qbic.projectmanagement.application;

import java.util.List;
import life.qbic.projectmanagement.application.api.PersonLookupService;
import life.qbic.projectmanagement.domain.project.PersonReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Person Search Service
 *
 * @since 1.0.0
 */
@Component
public class PersonSearchService {

  private final PersonLookupService personLookupService;

  public PersonSearchService(@Autowired PersonLookupService personLookupService) {
    this.personLookupService = personLookupService;
  }

  /**
   * Lists all findings for a person based on a provided filter expression.
   * <p>
   * Supports pagination by the parameters <code>offset</code> and
   * <code>limit</code>.
   *
   * @param filter a character sequence to filter the person references output for
   * @param offset a pagination offset
   * @param limit  a pagination limit
   * @return a list of {@link PersonReference} matching the search filter
   * @since 1.0.0
   */
  public List<PersonReference> find(String filter, int offset, int limit) {
    return personLookupService.find(filter, offset, limit);
  }
}
