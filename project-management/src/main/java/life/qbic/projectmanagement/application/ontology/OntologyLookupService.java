package life.qbic.projectmanagement.application.ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.repository.OntologyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic ontology information
 *
 * @since 1.0.0
 */
@Service
public class OntologyLookupService {

  private static final Logger log = LoggerFactory.logger(OntologyLookupService.class);
  private final OntologyLookupInterface ontologyTermLookup;
  private final OntologyRepository ontologyTermRepository;

  public OntologyLookupService(@Autowired OntologyLookupInterface ontologyTermLookup,
      @Autowired OntologyRepository ontologyTermRepository) {
    Objects.requireNonNull(ontologyTermLookup);
    this.ontologyTermLookup = ontologyTermLookup;
    this.ontologyTermRepository = ontologyTermRepository;
  }

  /**
   * Queries {@link OntologyClass}s with a provided offset and limit that supports pagination.
   *
   * @param termFilter the user's input will be applied to filter results
   * @param ontologyAbbreviations a List of ontology abbreviations denoting the ontology to search in
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   * @since 1.0.0
   */
  public List<OntologyClass> queryOntologyTerm(String termFilter, List<String> ontologyAbbreviations,
      int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<OntologyClass> termList = ontologyTermLookup.query(termFilter, ontologyAbbreviations, offset,
        limit, sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(termList);
  }

  public Optional<OntologyClass> find(Long id) {
    Objects.requireNonNull(id);
    return ontologyTermRepository.find(id);
  }

  public Optional<OntologyClass> findByCURI(String curi) {
    return ontologyTermRepository.findByCuri(curi);
  }

  public Optional<OntologyClass> find(String id) throws IllegalArgumentException{
    return find(Long.parseLong(id));
  }

}
