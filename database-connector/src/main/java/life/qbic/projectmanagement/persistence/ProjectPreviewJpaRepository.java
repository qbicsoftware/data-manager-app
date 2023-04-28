package life.qbic.projectmanagement.persistence;

import life.qbic.persistence.OffsetBasedRequest;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
@Scope("singleton")
public class ProjectPreviewJpaRepository implements ProjectPreviewLookup {

  private final ProjectPreviewRepository projectPreviewRepository;

  public ProjectPreviewJpaRepository(ProjectPreviewRepository projectPreviewRepository) {
    Objects.requireNonNull(projectPreviewRepository);
    this.projectPreviewRepository = projectPreviewRepository;
  }

  @Override
  public List<ProjectPreview> query(int offset, int limit) {
    return projectPreviewRepository.findAll(new OffsetBasedRequest(offset, limit)).getContent();
  }

  @Override
  public List<ProjectPreview> query(String filter, int offset, int limit,
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
