package life.qbic.projectmanagement.infrastructure.project;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
@Scope("singleton")
public class ProjectPreviewLookupImplementation implements ProjectPreviewLookup {

  private final ProjectPreviewRepository projectPreviewRepository;

  public ProjectPreviewLookupImplementation(ProjectPreviewRepository projectPreviewRepository) {
    Objects.requireNonNull(projectPreviewRepository);
    this.projectPreviewRepository = projectPreviewRepository;
  }

  @Override
  public List<ProjectPreview> query(int offset, int limit) {
    return projectPreviewRepository.findAll(new OffsetBasedRequest(offset, limit)).getContent();
  }

  @Override
  public List<ProjectPreview> query(String filter, int offset, int limit,
      List<SortOrder> sortOrders, Collection<ProjectId> projectIds) {
    List<Order> orders = sortOrders.stream().map(it -> {
      Order order;
      if (it.isDescending()) {
        order = Order.desc(it.propertyName());
      } else {
        order = Order.asc(it.propertyName());
      }
      return order;
    }).toList();
    Specification<ProjectPreview> filterSpecification = generateProjectPreviewSpecification(
        projectIds, filter);
    return projectPreviewRepository.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  private Specification<ProjectPreview> generateProjectPreviewSpecification(
      Collection<ProjectId> projectIds, String filter) {
    Specification<ProjectPreview> isBlankSpec = ProjectPreviewSpec.isBlank(filter);
    Specification<ProjectPreview> isDistinctSpec = ProjectPreviewSpec.isDistinct();
    Specification<ProjectPreview> containsProjectId = ProjectPreviewSpec.containsProjectId(
        projectIds);
    Specification<ProjectPreview> isProjectTitle = ProjectPreviewSpec.isProjectTitle(filter);
    Specification<ProjectPreview> isProjectCode = ProjectPreviewSpec.isProjectCode(filter);
    Specification<ProjectPreview> isLastModifiedDate = ProjectPreviewSpec.isLastModifiedDate(
        filter);
    Specification<ProjectPreview> filterSpecification =
        Specification.anyOf(isProjectTitle, isProjectCode, isLastModifiedDate);
    return Specification.where(isBlankSpec)
        .and(containsProjectId)
        .and(filterSpecification)
        .and(isDistinctSpec);
  }

  private static class ProjectPreviewSpec {

    //We need to ensure that we only count and retrieve unique ngsMeasurements
    public static Specification<ProjectPreview> isDistinct() {
      return (root, query, builder) -> {
        query.distinct(true);
        return null;
      };
    }

    //We are only interested in measurements which contain at least one of the provided sampleIds
    public static Specification<ProjectPreview> containsProjectId(
        Collection<ProjectId> projectIds) {
      return (root, query, builder) -> {
        if (projectIds.isEmpty()) {
          //If no sampleId is in the experiment then there can also be no measurement
          return builder.disjunction();
        } else {
          return root.join("id").in(projectIds);
        }
      };
    }

    //If no filter was provided return all proteomicsMeasurement
    public static Specification<ProjectPreview> isBlank(String filter) {
      return (root, query, builder) -> {
        if (filter != null && filter.isBlank()) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<ProjectPreview> isProjectCode(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("projectCode").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProjectPreview> isProjectTitle(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("projectTitle"), "%" + filter + "%");
    }

    public static Specification<ProjectPreview> isLastModifiedDate(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("lastModified").as(String.class), "%" + filter + "%");
    }
  }

}
