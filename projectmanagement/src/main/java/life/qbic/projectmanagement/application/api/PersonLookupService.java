package life.qbic.projectmanagement.application.api;

import java.util.List;
import life.qbic.projectmanagement.domain.project.PersonReference;

public interface PersonLookupService {

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
  List<PersonReference> find(String filter, int offset, int limit);

}
