package life.qbic.projectmanagement.infrastructure.experiment;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.api.OntologyTermLookup;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.domain.repository.OntologyTermRepository;
import life.qbic.projectmanagement.infrastructure.OffsetBasedRequest;
import life.qbic.projectmanagement.infrastructure.project.ProjectPreviewRepository;
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

  @Override
  public List<OntologyClassEntity> query(String filter, int offset, int limit,
      List<SortOrder> sortOrders) {
    List<Order> orders = sortOrders.stream().map(it -> {
      Order order;
      if (it.isDescending()) {
        order = Order.desc(it.propertyName());
      } else {
        order = Order.asc(it.propertyName());
      }
      return order;
    }).toList();
    return projectPreviewRepository.findByProjectTitleContainingIgnoreCaseOrProjectCodeContainingIgnoreCase(
        filter, filter, new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

}
