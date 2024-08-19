package life.qbic.projectmanagement.application.ontology;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>API for ontology term selection or lookup for autocomplete use cases</b>
 *
 * @since 1.4.0
 */
public interface TerminologySelect {

  /**
   * Queries possible matching ontology terms given the provided search term. This method should be
   * used by clients that want to support an auto-suggest use case. The implementation needs to
   * optimise for fast lookup times.
   * <p>
   * <b>NOTE!</b>
   * <p>
   * The resulting {@link OntologyTerm} objects are not guaranteed to contain values for properties
   * like "description". This depends highly on the implementation, use the
   * {@link TerminologySelect#search(String, int, int)} method for a rich search.
   *
   * @param searchTerm a full or partial term that the client wants to select from potentials search
   *                   hits
   * @param offset     0 for starting the listing from the beginning of all possible matches, or
   *                   slice through the results with an offset
   * @param limit      the maximum number of matches returned per search
   * @return a list of matching terms given the provided search term
   * @since 1.4.0
   */
  List<OntologyClass> query(String searchTerm, int offset, int limit);

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
  Optional<OntologyClass> searchByCurie(String curie);

  /**
   * Searches for possible matching ontology terms. This search returns rich {@link OntologyTerm}
   * objects, and should be used when information for properties like "description" needs to be
   * used.
   *
   * @param searchTerm a full or partial term that the client wants to select from potentials search
   *                   hits
   * @param offset     0 for starting the listing from the beginning of all possible matches, or
   *                   slice through the results with an offset
   * @param limit      the maximum number of matches returned per search
   * @return a list of matching terms given the provided search term.
   * @since 1.4.0
   */
  List<OntologyClass> search(String searchTerm, int offset, int limit);
}
