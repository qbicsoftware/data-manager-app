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
   * Queries possible matching ontology terms given the provided search term
   *
   * @param searchTerm a full or partial term that the client wants to select from potentials search
   *                   hits
   * @param offset     0 for starting the listing from the beginning of all possible matches, or
   *                   slice through the results with an offset
   * @param limit      the maximum number of matches returned per search
   * @return a list of matching terms given the provided search term
   * @since 1.4.0
   */
  List<OntologyTerm> search(String searchTerm, int offset, int limit);

  /**
   * Queries possible matching ontology terms given a provided CURIE, such as the OBO ID.
   *
   * @param curie  the CURIE of the term to search for
   * @param offset 0 for starting the listing from the beginning of all possible matches, or slice
   *               through the results with an offset
   * @param limit  the maximum number of matches returned per search
   * @return a list of matching terms given the provided CURIE
   * @since 1.4.0
   */
  List<OntologyTerm> searchByCurie(String curie, int offset, int limit);

}
