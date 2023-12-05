package life.qbic.projectmanagement.infrastructure.ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.api.OntologyTermLookup;
import life.qbic.projectmanagement.infrastructure.OffsetBasedRequest;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query ontology class information
 *
 * @since 1.0.0
 */
@Component
@Scope("singleton")
public class OntologyTermJpaRepository implements OntologyTermLookup {

  private final OntologyTermRepository ontologyTermRepository;

  public OntologyTermJpaRepository(OntologyTermRepository ontologyTermRepository) {
    Objects.requireNonNull(ontologyTermRepository);
    this.ontologyTermRepository = ontologyTermRepository;
  }

  @Override
  public List<OntologyClassEntity> query(int offset, int limit) {
    return ontologyTermRepository.findAll(new OffsetBasedRequest(offset, limit)).getContent();
  }

  /**
   * The way the MyISAM engine searches the fulltext index makes it necessary to use multiple
   * search terms that need to be found instead of one full search term.
   * The asterisk suffix is needed so incomplete words are found (mu for musculus).
   */
  private String buildSearchTerm(String searchString) {
    StringBuilder searchTermBuilder = new StringBuilder();
    for(String word : searchString.split(" ")) {
      searchTermBuilder.append(" +" + word);
    }
    searchTermBuilder.append("*");
    return searchTermBuilder.toString().trim();
  }

  @Override
  public List<OntologyClassEntity> query(String searchString, List<String> ontologies, int offset,
      int limit, List<SortOrder> sortOrders) {
    List<Order> orders = sortOrders.stream().map(it -> {
      Order order;
      if (it.isDescending()) {
        order = Order.desc(it.propertyName());
      } else {
        order = Order.asc(it.propertyName());
      }
      return order;
    }).toList();
    searchString = searchString.trim();
    if(searchString.length() < 2) {
      return new ArrayList<>();
    }
    // otherwise create a more complex search term for fulltext search
    String searchTerm = buildSearchTerm(searchString);
    return ontologyTermRepository.findByLabelFulltextMatching(
        searchTerm, ontologies, new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }
}