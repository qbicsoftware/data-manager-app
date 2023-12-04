package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.OntologyTermLookup;
import life.qbic.projectmanagement.domain.repository.OntologyTermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic ontology information
 *
 * @since 1.0.0
 */
@Service
public class OntologyTermInformationService {

  private static final Logger log = LoggerFactory.logger(OntologyTermInformationService.class);
  private final OntologyTermLookup ontologyTermLookup;
  private final OntologyTermRepository ontologyTermRepository;

  public OntologyTermInformationService(@Autowired OntologyTermLookup ontologyTermLookup,
      @Autowired OntologyTermRepository ontologyTermRepository) {
    Objects.requireNonNull(ontologyTermLookup);
    this.ontologyTermLookup = ontologyTermLookup;
    this.ontologyTermRepository = ontologyTermRepository;
  }

  /**
   * Queries {@link OntologyClassEntity}s with a provided offset and limit that supports pagination.
   *
   * @param termFilter the user's input will be applied to filter results
   * @param ontologies a List of ontology names denoting the ontologies to search in
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   * @since 1.0.0
   */
  public List<OntologyClassEntity> queryOntologyTerm(String termFilter, List<String> ontologies,
      int offset, int limit, List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<OntologyClassEntity> termList = ontologyTermLookup.query(termFilter, ontologies, offset,
        50, sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(termList);
  }

  public Optional<OntologyClassEntity> find(Long id) {
    Objects.requireNonNull(id);
    return ontologyTermRepository.find(id);
  }

  public Optional<OntologyClassEntity> find(String id) throws IllegalArgumentException{
    return find(Long.parseLong(id));
  }

}
