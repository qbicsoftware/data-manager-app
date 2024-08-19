package life.qbic.projectmanagement.application.ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ontology.OntologyLookupInterface.FilterTerm;
import life.qbic.projectmanagement.application.ontology.OntologyLookupInterface.OntologyCurie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to search basic ontology information
 *
 * @since 1.0.0
 */
@Service
public class OntologyLookupService {

  private final OntologyLookupInterface ontologyTermLookup;

  public OntologyLookupService(@Autowired OntologyLookupInterface ontologyTermLookup) {
    Objects.requireNonNull(ontologyTermLookup);
    this.ontologyTermLookup = ontologyTermLookup;
  }

  /**
   * Queries {@link OntologyClass}s with a provided offset and limit that supports pagination.
   *
   * @param filterTerm the user's input will be applied to filter results
   * @param ontologyAbbreviations a List of ontology abbreviations denoting the ontology to search in
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   * @since 1.0.0
   */
  public List<OntologyClass> queryOntologyTerm(String filterTerm,
      List<String> ontologyAbbreviations,
      int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<OntologyClass> termList = ontologyTermLookup.query(new FilterTerm(filterTerm),
        ontologyAbbreviations, offset, limit, sortOrders)
        .stream().distinct().toList();
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(termList);
  }

  public Optional<OntologyClass> findByCURI(String curie) {
    return ontologyTermLookup.query(new OntologyCurie(curie)).stream().findAny();
  }

  public List<String> findUniqueOntologies() {
    return ontologyTermLookup.findUniqueOntologyAbbreviations();
  }

}
