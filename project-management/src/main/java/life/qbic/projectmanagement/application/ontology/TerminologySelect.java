package life.qbic.projectmanagement.application.ontology;

import java.util.List;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>API for ontology term selection or lookup for autocomplete use cases</b>
 *
 * @since 1.4.0
 */
public interface TerminologySelect {

  /**
   * Queries a list of matching ontology terms given the provided search term
   *
   * @param searchTerm a full or partial term that the client wants to select from potentials search
   *                   hits
   * @param offset     0 for starting the listing from the beginning of all possible matches, or
   *                   slice through the results with an offset
   * @param limit      the maximum number or matches returned per query
   * @return a list of matching terms given the provided search term
   * @since 1.4.0
   */
  List<OntologyTerm> query(String searchTerm, int offset, int limit);

}
