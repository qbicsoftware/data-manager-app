package life.qbic.projectmanagement.infrastructure.query;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.projectmanagement.application.api.ProjectQueryService.ProjectSampleFilter;
import life.qbic.projectmanagement.application.api.ProjectQueryService.ProjectSampleSortKey;
import life.qbic.projectmanagement.application.api.ProjectQueryService.SortDirection;
import life.qbic.projectmanagement.application.api.ProjectQueryService.SortOrder;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.ProjectPageReadRepository;
import life.qbic.projectmanagement.application.query.ProjectSampleOverview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * JPA implementation of {@link ProjectPageReadRepository}, backed by the project-page read-model
 * views ({@code experiment_progress}, {@code v_project_sample_overview}).
 *
 * <p>Access control sits here, on the read port (mirroring {@code LocalRawDatasetLookupService}).
 * The caller of the port is the reactive {@link
 * life.qbic.projectmanagement.application.api.ProjectQueryService}, which propagates the security
 * context onto the scheduler thread, so the {@code @PreAuthorize} check evaluates against the real
 * principal.</p>
 *
 * @since 1.19.0
 */
@Component
public class ProjectPageReadRepositoryImpl implements ProjectPageReadRepository {

  private static final String PROJECT_READ = "hasPermission(#projectId, "
      + "'life.qbic.projectmanagement.domain.model.project.Project', 'READ')";
  private static final String SAMPLE_CODE = "sampleCode";

  private final ExperimentProgressRepository experimentProgressRepository;
  private final ProjectSampleOverviewRepository sampleOverviewRepository;

  public ProjectPageReadRepositoryImpl(
      @Autowired ExperimentProgressRepository experimentProgressRepository,
      @Autowired ProjectSampleOverviewRepository sampleOverviewRepository) {
    this.experimentProgressRepository = experimentProgressRepository;
    this.sampleOverviewRepository = sampleOverviewRepository;
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public List<ExperimentProgress> findExperimentProgress(String projectId) {
    return experimentProgressRepository.findAllByProjectId(projectId);
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public List<ProjectSampleOverview> findProjectSamples(String projectId, int offset, int limit,
      ProjectSampleFilter filter) {
    var pageable = new OffsetBasedRequest(offset, limit, sortOf(filter.sortOrders()));
    return sampleOverviewRepository.findAll(specification(projectId, filter), pageable).getContent();
  }

  @Override
  @PreAuthorize(PROJECT_READ)
  public long countProjectSamples(String projectId, ProjectSampleFilter filter) {
    return sampleOverviewRepository.count(specification(projectId, filter));
  }

  private static Specification<ProjectSampleOverview> specification(String projectId,
      ProjectSampleFilter filter) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("projectId"), projectId));
      if (filter.experimentId() != null) {
        predicates.add(builder.equal(root.get("experimentId"), filter.experimentId()));
      }
      if (!filter.sampleName().isBlank()) {
        String like = "%" + filter.sampleName().toLowerCase() + "%";
        predicates.add(builder.or(
            builder.like(builder.lower(root.get("sampleName").as(String.class)), like),
            builder.like(builder.lower(root.get(SAMPLE_CODE).as(String.class)), like)));
      }
      switch (filter.measured()) {
        case MEASURED -> predicates.add(builder.isTrue(root.get("measured").as(Boolean.class)));
        case NOT_MEASURED ->
            predicates.add(builder.isFalse(root.get("measured").as(Boolean.class)));
        case ALL -> {
          // no constraint
        }
      }
      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Translates the requested sort orders into a Spring Data {@link Sort}. The sample code is
   * appended as a deterministic tie-break unless it is already part of the sort, so pagination is
   * stable (ADR-0009).
   */
  private static Sort sortOf(List<SortOrder<ProjectSampleSortKey>> orders) {
    List<Sort.Order> sortOrders = new ArrayList<>();
    for (SortOrder<ProjectSampleSortKey> order : orders) {
      sortOrders.add(new Sort.Order(directionOf(order.direction()), propertyOf(order.key())));
    }
    boolean sampleCodeSelected = sortOrders.stream()
        .anyMatch(order -> order.getProperty().equals(SAMPLE_CODE));
    if (!sampleCodeSelected) {
      sortOrders.add(new Sort.Order(Sort.Direction.ASC, SAMPLE_CODE));
    }
    return Sort.by(sortOrders);
  }

  private static Sort.Direction directionOf(SortDirection direction) {
    return direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
  }

  private static String propertyOf(ProjectSampleSortKey key) {
    return switch (key) {
      case SAMPLE_ID -> SAMPLE_CODE;
      case SAMPLE_NAME -> "sampleName";
      case EXPERIMENT -> "experimentName";
      case REGISTRATION_TIME -> "registrationTime";
      case MODIFICATION_TIME -> "lastModified";
    };
  }
}
