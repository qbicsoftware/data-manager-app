package life.qbic.projectmanagement.experiment.persistence;

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

    Specification<SamplePreview> sampleCodeSpec = SamplePreviewSpecs.sampleCodeContains(filter);
    Specification<SamplePreview> sampleLabelSpec = SamplePreviewSpecs.sampleLabelContains(filter);
    Specification<SamplePreview> batchLabelSpec = SamplePreviewSpecs.batchLabelContains(filter);
    Specification<SamplePreview> bioReplicateLabelSpec = SamplePreviewSpecs.BioReplicateLabelContains(
        filter);
    /*
    Specification<SamplePreview> experimentalGroupSpec = SamplePreviewSpecs.experimentalGroupsEquals(
        filter);
    Specification<SamplePreview> speciesSpec = SamplePreviewSpecs.speciesEquals(filter);
    Specification<SamplePreview> specimenSpec = SamplePreviewSpecs.specimenEquals(filter);
    Specification<SamplePreview> analyteSpec = SamplePreviewSpecs.analyteEquals(filter);
    */
    Specification<SamplePreview> combinedSpec = Specification.anyOf(sampleCodeSpec, sampleLabelSpec,
        batchLabelSpec, bioReplicateLabelSpec);
    /* experimentalGroupSpec,
    speciesSpec, specimenSpec, analyteSpec*/
    return samplePreviewRepository.findAll(combinedSpec,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
    /*return samplePreviewRepository.findSamplePreviewByExperimentId(experimentId,
        new OffsetBasedRequest(offset, limit, Sort.by(orders)), filter).getContent(); */
  }

  @Override
  public int queryCountByExperimentId(ExperimentId experimentId) {
    return samplePreviewRepository.countSamplePreviewsByExperimentId(experimentId);
  }

  private static class SamplePreviewSpecs {

    public static Specification<SamplePreview> sampleCodeContains(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("sampleCode"), filter);
    }


    public static Specification<SamplePreview> batchLabelContains(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("batchLabel"), filter);
    }

    public static Specification<SamplePreview> BioReplicateLabelContains(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("bioReplicateLabel"), filter);
    }

    public static Specification<SamplePreview> sampleLabelContains(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("sampleLabel"), filter);
    }
/*
    public static Specification<SamplePreview> experimentalGroupsEquals(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("experimentalGroup"), filter);
    }

    public static Specification<SamplePreview> speciesEquals(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("species").get("label"), filter);
    }

    public static Specification<SamplePreview> specimenEquals(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("specimen").get("label"), filter);
    }

    public static Specification<SamplePreview> analyteEquals(String filter) {
      return (root, query, builder) ->
          StringUtil.isBlank(filter) ?
              builder.conjunction() :
              builder.like(root.get("analyte"), filter);
    }

 */
  }
}
