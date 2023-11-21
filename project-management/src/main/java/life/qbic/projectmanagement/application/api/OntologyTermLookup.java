package life.qbic.projectmanagement.application.api;

import java.util.List;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.application.SortOrder;

public interface OntologyTermLookup {

  /**
   * Queries ontology classes with a provided offset and limit that supports pagination.
   *
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<OntologyClassEntity> query(int offset, int limit);

  /**
   * Queries ontology classes with a provided offset and limit that supports pagination.
   *
   * @param termFilter the user's input will be applied to filter results
   * @param ontologies a List of ontology names denoting the ontologies to search in
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the ordering to sort by
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<OntologyClassEntity> query(String termFilter, List<String> ontologies, int offset, int limit,
      List<SortOrder> sortOrders);

}
