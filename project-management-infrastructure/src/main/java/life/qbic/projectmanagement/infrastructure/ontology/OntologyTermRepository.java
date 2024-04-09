package life.qbic.projectmanagement.infrastructure.ontology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.OntologyLookupInterface;
import life.qbic.projectmanagement.domain.repository.OntologyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;


/**
 * <b>Ontology term repository implementation</b>
 *
 * <p>Implementation for the {@link OntologyRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link OntologyClass} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link OntologyTermRepositoryJpaInterface}, which is injected as
 * dependency upon creation.
 * <p>
 *
 * @since 1.0.0
 */
@Service
public class OntologyTermRepository implements OntologyRepository, OntologyLookupInterface {

  private final OntologyTermRepositoryJpaInterface jpaRepository;

  @Autowired
  public OntologyTermRepository(OntologyTermRepositoryJpaInterface jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<OntologyClass> findByCuri(String curie) {
    return jpaRepository.findOntologyClassEntitiesByCurie(curie).stream().findAny();
  }

  /**
   * The way the MyISAM engine searches the fulltext index makes it necessary to use multiple search
   * terms that need to be found instead of one full search term. The asterisk suffix is needed so
   * incomplete words are found (mu for musculus).
   */
  private String buildSearchTerm(String searchString) {
    StringBuilder searchTermBuilder = new StringBuilder();
    for (String word : searchString.split(" ")) {
      searchTermBuilder.append(" +").append(word);
    }
    searchTermBuilder.append("*");
    return searchTermBuilder.toString().trim();
  }

  @Override
  public List<OntologyClass> query(FilterTerm filterTerm,
      List<String> ontologyAbbreviations,
      int offset,
      int limit, List<SortOrder> sortOrders) {

    var searchString = filterTerm.term().trim();
    if (searchString.length() < 2) {
      return new ArrayList<>();
    }
    // otherwise create a more complex search term for fulltext search
    String searchTerm = buildSearchTerm(searchString);
    OffsetBasedRequest pageable = new OffsetBasedRequest(offset, limit, sortByOrders(sortOrders));
    return jpaRepository.findByLabelFulltextMatching(searchTerm, ontologyAbbreviations,
            pageable)
        .getContent();
  }

  @Override
  public Collection<OntologyClass> query(OntologyCurie ontologyCurie) {
    // The CURIE (aka "name" in the database) is currently formatted with an "_" (underscore) as delimiter
    var delimiterCorrectedCURI = ontologyCurie.curie().trim().replace(":", "_");
    // And the prefix is in capitalised form
    var capitalizedCURI = delimiterCorrectedCURI.toUpperCase();

    return jpaRepository.findByCuriFulltextMatching(capitalizedCURI);
  }

  private static Sort sortByOrders(List<SortOrder> sortOrders) {
    return Sort.by(sortOrders.stream()
        .map(sortOrder -> sortOrder.isDescending()
            ? Order.desc(sortOrder.propertyName())
            : Order.asc(sortOrder.propertyName()))
        .toList());
  }

  public List<String> findUniqueOntologyAbbreviations() {
    return jpaRepository.findUniqueOntologies();
  }
}
