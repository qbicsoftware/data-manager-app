package life.qbic.projectmanagement.application.ontology;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.application.SortOrder;

/**
 * Ontology Lookup interface that needs to be implemented by ontology providers
 *
 * @since 1.0.0
 */
public interface OntologyLookupInterface {

  /**
   * Queries ontology classes with a provided offset and limit that supports pagination.
   *
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<OntologyClass> query(int offset, int limit);

  /**
   * Queries ontology classes with a provided offset and limit that supports pagination.
   *
   * @param termFilter            the user's input will be applied to filter results
   * @param ontologyAbbreviations a List of ontology abbreviations denoting the ontology to search
   *                              in
   * @param offset                the offset for the search result to start
   * @param limit                 the maximum number of results that should be returned
   * @param sortOrders            the ordering to sort by
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<OntologyClass> query(String termFilter, List<String> ontologyAbbreviations, int offset,
      int limit,
      List<SortOrder> sortOrders);

  /**
   * Provides all matching {@link OntologyClass}es for ontologies with the provided CURI.
   * <p>
   * CURI: compact uniform resource identifier
   * <p>
   * Expected CURI format: [prefix][delimiter][local identifier]
   * <p>
   * Supported delimiter: <code>":"</code> (colon) or <code>"_"</code> (underscore)
   * <p>
   * CURI examples:
   *
   * <ul>
   *   <li>
   *     CHEBI:138488
   *   </li>
   *   <li>
   *     CHEBI_138488
   *   </li>
   * </ul>
   * <strong>The search must be performed case-insensitive!</strong>
   *
   * @param ontologyCURI the CURI to find the corresponding ontology class
   * @return
   * @since 1.0.0
   */
  Collection<OntologyClass> query(String ontologyCURI);

}
