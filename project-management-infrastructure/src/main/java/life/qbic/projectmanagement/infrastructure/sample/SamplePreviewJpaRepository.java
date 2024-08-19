package life.qbic.projectmanagement.infrastructure.sample;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SamplePreviewLookup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to search sample preview information
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
  public List<SamplePreview> queryByExperimentId(ExperimentId experimentId) {
    return samplePreviewRepository.findAll(SamplePreviewSpecs.experimentIdEquals(experimentId));
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
    Specification<SamplePreview> organismIdSpec = SamplePreviewSpecs.organismIdContains(filter);
    Specification<SamplePreview> sampleCodeSpec = SamplePreviewSpecs.sampleCodeContains(filter);
    Specification<SamplePreview> sampleNameSpec = SamplePreviewSpecs.sampleNameContains(filter);
    Specification<SamplePreview> batchLabelSpec = SamplePreviewSpecs.batchLabelContains(filter);
    Specification<SamplePreview> conditionSpec = SamplePreviewSpecs.conditionContains(filter);
    Specification<SamplePreview> speciesSpec = SamplePreviewSpecs.speciesContains(filter);
    Specification<SamplePreview> specimenSpec = SamplePreviewSpecs.specimenContains(filter);
    Specification<SamplePreview> analyteSpec = SamplePreviewSpecs.analyteContains(filter);
    Specification<SamplePreview> analysisMethodContains = SamplePreviewSpecs.analysisMethodContains(
        filter);
    Specification<SamplePreview> commentSpec = SamplePreviewSpecs.commentContains(filter);
    Specification<SamplePreview> containsFilterSpec = Specification.anyOf(sampleCodeSpec,
        sampleNameSpec, organismIdSpec, batchLabelSpec, conditionSpec, speciesSpec,
        specimenSpec, analyteSpec, analysisMethodContains, commentSpec);
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
        if (filter != null && filter.isBlank()) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<SamplePreview> experimentIdEquals(ExperimentId experimentId) {
      var id = experimentId == null ? "" : experimentId.value();
      return (root, query, builder) ->
          id.isBlank() ?
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

    public static Specification<SamplePreview> sampleNameContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("sampleName"), "%" + filter + "%");
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

    private static Specification<SamplePreview> ontologyColumnContains(String col, String filter) {
      return (root, query, builder) -> {
        /*We're currently only interested in the label within the json file*/
        Expression<String> function = builder.function("JSON_EXTRACT", String.class, root.get(col),
            builder.literal("$.label"));
        return builder.like(function,
            "%" + filter + "%");
      };
    }

    public static Specification<SamplePreview> organismIdContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("organismId"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> speciesContains(String filter) {
      return ontologyColumnContains("species", filter);
    }

    public static Specification<SamplePreview> specimenContains(String filter) {
      return ontologyColumnContains("specimen", filter);
    }

    public static Specification<SamplePreview> analyteContains(String filter) {
      return ontologyColumnContains("analyte", filter);
    }

    public static Specification<SamplePreview> analysisMethodContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("analysisMethod"), "%" + filter + "%");
    }

    public static Specification<SamplePreview> commentContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("comment"), "%" + filter + "%");
    }

  }
}
