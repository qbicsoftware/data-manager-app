package life.qbic.projectmanagement.infrastructure.ontology;

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
   */
  private String buildSearchTerm(String searchString) {
    String searchTerm = "";
    for(String word : searchString.split(" ")) {
      searchTerm += " +" + word;
    }
    return searchTerm+"*";
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
    String searchTerm = buildSearchTerm(searchString);
    return ontologyTermRepository.findByLabelContainingIgnoreCaseAndOntologyIn(
        searchTerm, ontologies, new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }
}
