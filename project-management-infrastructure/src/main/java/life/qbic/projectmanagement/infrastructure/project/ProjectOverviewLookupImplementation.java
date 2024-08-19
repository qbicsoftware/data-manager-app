package life.qbic.projectmanagement.infrastructure.project;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.api.ProjectOverviewLookup;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to search project overview information
 */
@Component
@Scope("singleton")
public class ProjectOverviewLookupImplementation implements ProjectOverviewLookup {

  private final ProjectOverviewRepository projectOverviewRepository;

  public ProjectOverviewLookupImplementation(ProjectOverviewRepository projectOverviewRepository) {
    Objects.requireNonNull(projectOverviewRepository);
    this.projectOverviewRepository = projectOverviewRepository;
  }

  @Override
  public List<ProjectOverview> query(int offset, int limit) {
    return projectOverviewRepository.findAll(new OffsetBasedRequest(offset, limit)).getContent();
  }

  @Override
  public List<ProjectOverview> query(String filter, int offset, int limit,
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
    Specification<ProjectOverview> filterSpecification = generateProjectOverviewSpecification(
        projectIds, filter);
    return projectOverviewRepository.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  private Specification<ProjectOverview> generateProjectOverviewSpecification(
      Collection<ProjectId> projectIds, String filter) {
    Specification<ProjectOverview> isBlankSpec = ProjectOverviewSpec.isBlank(filter);
    Specification<ProjectOverview> isDistinctSpec = ProjectOverviewSpec.isDistinct();
    Specification<ProjectOverview> containsProjectId = ProjectOverviewSpec.containsProjectId(
        projectIds);
    Specification<ProjectOverview> isProjectTitle = ProjectOverviewSpec.isProjectTitle(filter);
    Specification<ProjectOverview> isProjectCode = ProjectOverviewSpec.isProjectCode(filter);
    Specification<ProjectOverview> isLastModifiedDate = ProjectOverviewSpec.isLastModifiedDate(
        filter);
    Specification<ProjectOverview> isPrincipalInvestigator = ProjectOverviewSpec.isPrincipalInvestigator(
        filter);
    Specification<ProjectOverview> isProjectManager = ProjectOverviewSpec.isProjectManager(
        filter);
    Specification<ProjectOverview> isResponsiblePerson = ProjectOverviewSpec.isProjectResponsible(
        filter);
    Specification<ProjectOverview> hasNgsMeasurements = ProjectOverviewSpec.hasNgsMeasurements(
        filter);
    Specification<ProjectOverview> hasPxpMeasurements = ProjectOverviewSpec.hasPxPMeasurements(
        filter);
    Specification<ProjectOverview> isInCollaboratorNames = ProjectOverviewSpec.isInCollaboratorNames(
        filter);
    Specification<ProjectOverview> filterSpecification = Specification.anyOf(isProjectTitle,
        isProjectCode, isLastModifiedDate, isPrincipalInvestigator, isResponsiblePerson,
        hasNgsMeasurements, hasPxpMeasurements, isInCollaboratorNames);
    return Specification.where(isBlankSpec)
        .and(containsProjectId)
        .and(filterSpecification)
        .and(isDistinctSpec);
  }

  private static class ProjectOverviewSpec {

    //We need to ensure that we only count and retrieve unique project overviews
    public static Specification<ProjectOverview> isDistinct() {
      return (root, query, builder) -> {
        query.distinct(true);
        return null;
      };
    }

    //We are only interested in overviews which have one of the provided projectIds
    public static Specification<ProjectOverview> containsProjectId(
        Collection<ProjectId> projectIds) {
      return (root, query, builder) -> {
        if (projectIds.isEmpty()) {
          //If no projectIds are provided then the user has no access to any project
          return builder.disjunction();
        } else {
          return root.join("id").in(projectIds);
        }
      };
    }

    //If no filter was provided return all projectIds,
    //should always be combined with the containsProjectId specification
    public static Specification<ProjectOverview> isBlank(String filter) {
      return (root, query, builder) -> {
        if (filter != null && filter.isBlank()) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<ProjectOverview> isProjectCode(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("projectCode").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProjectOverview> isProjectTitle(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("projectTitle"), "%" + filter + "%");
    }

    public static Specification<ProjectOverview> isLastModifiedDate(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("lastModified").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProjectOverview> isPrincipalInvestigator(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("principalInvestigatorName").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProjectOverview> isProjectManager(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("projectManagerFullName").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProjectOverview> isProjectResponsible(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("projectResponsibleName").as(String.class), "%" + filter + "%");
    }

    public static Specification<ProjectOverview> hasNgsMeasurements(String filter) {
      return (root, query, builder) -> {
        //If the user does not filter for a substring of "genomics" this specification is irrelevant
        if (!"genomics".contains(filter)) {
          return builder.disjunction();
        }
        return builder.greaterThan(root.get("ngsMeasurementCount").as(Integer.class), 0);
      };
    }

    public static Specification<ProjectOverview> hasPxPMeasurements(String filter) {
      return (root, query, builder) -> {
        //If the user does not filter for a substring of proteomics this specification is irrelevant
        if (!"proteomics".contains(filter)) {
          return builder.disjunction();
        }
        return builder.greaterThan(root.get("pxpMeasurementCount").as(Integer.class), 0);
      };
    }

    public static Specification<ProjectOverview> isInCollaboratorNames(String filter) {
      return (root, query, builder) -> builder.like(root.get("collaboratorUserInfos"),
          "%" + filter + "%");

    }
  }
}
