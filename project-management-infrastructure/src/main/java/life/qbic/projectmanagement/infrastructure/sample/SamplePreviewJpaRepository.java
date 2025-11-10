package life.qbic.projectmanagement.infrastructure.sample;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewFilter;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewSortKey;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SamplePreviewLookup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
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
  public List<SamplePreview> queryByExperimentId(ExperimentId experimentId) {
    return samplePreviewRepository.findAll(SamplePreviewSpecs.experimentIdEquals(experimentId));
  }

  @Override
  public int queryCountByExperimentId(ExperimentId experimentId, String filter) {
    Specification<SamplePreview> filterSpecification = generateExperimentIdandFilterSpecification(
        experimentId, filter);
    return (int) samplePreviewRepository.count(filterSpecification);
  }

  @Override
  public List<SamplePreview> queryByExperimentId(String experimentId, int offset, int limit,
      SamplePreviewFilter filter) {
    List<Order> orders;

    try {
      orders = filter.sortOrders().stream().map(SamplePreviewJpaRepository::fromAPItoJpaSamplePreview).toList();
    } catch (IllegalArgumentException e) {
      throw new LookupException("Query for sample previews failed", e);
    }

    Specification<SamplePreview> filterSpecification = generateExperimentIdandFilterSpecification(
        ExperimentId.parse(experimentId), filter.sampleName());
    return samplePreviewRepository.findAll(filterSpecification,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  private static final Map<SamplePreviewSortKey, String> samplePreviewSortKeys =
      new EnumMap<>(SamplePreviewSortKey.class);

  static {
    samplePreviewSortKeys.put(SamplePreviewSortKey.SAMPLE_ID, "sampleCode");
    samplePreviewSortKeys.put(SamplePreviewSortKey.SAMPLE_NAME, "sampleName");
    samplePreviewSortKeys.put(SamplePreviewSortKey.BIOLOGICAL_REPLICATE, "biologicalReplicate");
    samplePreviewSortKeys.put(SamplePreviewSortKey.CONDITION, "experimentalGroup");
    samplePreviewSortKeys.put(SamplePreviewSortKey.BATCH, "batchLabel");
    samplePreviewSortKeys.put(SamplePreviewSortKey.SPECIES, "species");
    samplePreviewSortKeys.put(SamplePreviewSortKey.SPECIMEN, "specimen");
    samplePreviewSortKeys.put(SamplePreviewSortKey.ANALYTE, "analyte");
    samplePreviewSortKeys.put(SamplePreviewSortKey.ANALYSIS_METHOD, "analysisMethod");
    samplePreviewSortKeys.put(SamplePreviewSortKey.COMMENT, "comment");
  }

  private static Order fromAPItoJpaSamplePreview(
      AsyncProjectService.SortOrder<SamplePreviewSortKey> sortOrder) {
    var mappedJpaProperty = propertyFromAPItoJpaSamplePreview(sortOrder.key()).orElseThrow(
        () -> new IllegalArgumentException(
            "Cannot map key to JPA samplePreview property: " + sortOrder.key()));
    if (sortOrder.direction() ==  SortDirection.ASC) {
      return Order.asc(mappedJpaProperty);
    } else {
      return Order.desc(mappedJpaProperty);
    }
  }

  private static Optional<String> propertyFromAPItoJpaSamplePreview(
      SamplePreviewSortKey samplePreview) {
    return Optional.ofNullable(samplePreviewSortKeys.getOrDefault(samplePreview, null));
  }

  private Specification<SamplePreview> generateExperimentIdandFilterSpecification(
      ExperimentId experimentId, String filter) {
    Specification<SamplePreview> isBlankSpec = SamplePreviewSpecs.isBlank(filter);
    Specification<SamplePreview> experimentIdSpec = SamplePreviewSpecs.experimentIdEquals(
        experimentId);
    Specification<SamplePreview> biologialReplicateSpec = SamplePreviewSpecs.biologicalReplicateContains(
        filter);
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
        sampleNameSpec, biologialReplicateSpec, batchLabelSpec, conditionSpec, speciesSpec,
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

    public static Specification<SamplePreview> biologicalReplicateContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("biologicalReplicate"), "%" + filter + "%");
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
      return (root, query, builder) -> {
        Set<String> matchingValues = Arrays.stream(AnalysisMethod.values())
            .filter(method -> method.label().toUpperCase().contains(filter.toUpperCase()))
            .map(AnalysisMethod::abbreviation)
            .collect(Collectors.toUnmodifiableSet());
        return root.get("analysisMethod").in(matchingValues);
      };
    }

    public static Specification<SamplePreview> commentContains(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("comment"), "%" + filter + "%");
    }

  }
}
