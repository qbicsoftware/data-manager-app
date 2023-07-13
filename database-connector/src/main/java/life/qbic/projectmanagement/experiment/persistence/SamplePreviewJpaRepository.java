package life.qbic.projectmanagement.experiment.persistence;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Objects;
import life.qbic.persistence.OffsetBasedRequest;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SamplePreviewLookup;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query sample preview information
 *
 * @since 1.0.0
 */
@Component
@Scope("singleton")
public class SamplePreviewJpaRepository implements SamplePreviewLookup {

  private final SamplePreviewRepository samplePreviewRepository;

  public SamplePreviewJpaRepository(SamplePreviewRepository samplePreviewRepository) {
    Objects.requireNonNull(samplePreviewRepository);
    this.samplePreviewRepository = samplePreviewRepository;
  }

  @Override
  public List<SamplePreview> queryByExperimentId(ExperimentId experimentId, int offset, int limit,
      List<SortOrder> sortOrders, String filter) {
    List<Order> orders = sortOrders.stream().map(it -> {
      Order order;
      if (it.isDescending()) {
        order = Order.desc(it.propertyName());
      } else {
        order = Order.asc(it.propertyName());
      }
      return order;
    }).toList();

    Specification<SamplePreview> filterSpecification = generateExperimentIdandFilterSpecification(
        experimentId, filter);
    return samplePreviewRepository.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  @Override
  public int queryCountByExperimentId(ExperimentId experimentId, String filter) {
    Specification<SamplePreview> filterSpecification = generateExperimentIdandFilterSpecification(
        experimentId, filter);
    return (int) samplePreviewRepository.count(filterSpecification);
  }

  private Specification<SamplePreview> generateExperimentIdandFilterSpecification(
      ExperimentId experimentId, String filter) {
    Specification<SamplePreview> isBlankSpec = SamplePreviewSpecs.isBlank(filter);
    Specification<SamplePreview> experimentIdSpec = SamplePreviewSpecs.experimentIdEquals(
        experimentId);
    Specification<SamplePreview> sampleCodeSpec = SamplePreviewSpecs.sampleCodeContains(filter);
    Specification<SamplePreview> sampleLabelSpec = SamplePreviewSpecs.sampleLabelContains(filter);
    Specification<SamplePreview> batchLabelSpec = SamplePreviewSpecs.batchLabelContains(filter);
    Specification<SamplePreview> bioReplicateLabelSpec = SamplePreviewSpecs.bioReplicateLabelContains(
        filter);
    Specification<SamplePreview> conditionSpec = SamplePreviewSpecs.conditionContains(filter);
    Specification<SamplePreview> speciesSpec = SamplePreviewSpecs.speciesContains(filter);
    Specification<SamplePreview> specimenSpec = SamplePreviewSpecs.specimenContains(filter);
    Specification<SamplePreview> analyteSpec = SamplePreviewSpecs.analyteContains(filter);
    Specification<SamplePreview> analysisTypeSpec = SamplePreviewSpecs.analysisTypeContains(filter);
    Specification<SamplePreview> commentSpec = SamplePreviewSpecs.commentContains(filter);
    Specification<SamplePreview> containsFilterSpec = Specification.anyOf(sampleCodeSpec,
        sampleLabelSpec, batchLabelSpec, bioReplicateLabelSpec, conditionSpec, speciesSpec,
        specimenSpec, analyteSpec, analysisTypeSpec, commentSpec);
    Specification<SamplePreview> isDistinctSpec = SamplePreviewSpecs.isDistinct();
    return Specification.where(experimentIdSpec).and(isBlankSpec)
        .and(containsFilterSpec)
        .and(isDistinctSpec);
  }

  private static class SamplePreviewSpecs {

    //We need to ensure that we only count and retrieve unique samplePreviews
    public static Specification<SamplePreview> isDistinct() {
      return (root, query, builder) -> {
        query.distinct(true);
        return null;
      };
    }

    //If no filter was provided return all SamplePreviews
    public static Specification<SamplePreview> isBlank(String filter) {
      return (root, query, builder) -> {
        if (StringUtil.isBlank(filter)) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<SamplePreview> experimentIdEquals(ExperimentId experimentId) {
      return (root, query, builder) ->
          StringUtil.isBlank(experimentId.value()) ?
              builder.conjunction() :
              builder.equal(root.get("experimentId"), experimentId);
    }

    public static Specification<SamplePreview> sampleCodeContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("sampleCode"), "%" + filter + "%");
    }


    public static Specification<SamplePreview> batchLabelContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("batchLabel"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> bioReplicateLabelContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("bioReplicateLabel"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> sampleLabelContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("sampleLabel"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> conditionContains(String filter) {
      return (root, query, builder) -> {
        Join<?, ?> expVariablesJoin = root.join("experimentalGroup").join("condition")
            .join("variableLevels");
        Expression<String> varNameExp = expVariablesJoin.get("variableName").as(String.class);
        Expression<String> varValueValueExp = expVariablesJoin.get("experimentalValue").get("value")
            .as(String.class);
        Expression<String> varUnitExp = expVariablesJoin.get("experimentalValue").get("unit")
            .as(String.class);
        Predicate varValuePred = builder.like(varValueValueExp, "%" + filter + "%");
        Predicate varUnitPred = builder.like(varUnitExp, "%" + filter + "%");
        Predicate varNamePred = builder.like(varNameExp, "%" + filter + "%");
        return builder.or(varNamePred, varValuePred, varUnitPred);
      };
    }

    public static Specification<SamplePreview> speciesContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("species"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> specimenContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("specimen"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> analyteContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("analyte"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> analysisTypeContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("analysisType"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> commentContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("comment"), "%" + filter + "%");
    }

  }
}
