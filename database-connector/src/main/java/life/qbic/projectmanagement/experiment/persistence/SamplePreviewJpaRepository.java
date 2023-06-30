package life.qbic.projectmanagement.experiment.persistence;

import java.util.List;
import java.util.Objects;
import life.qbic.persistence.OffsetBasedRequest;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SamplePreviewLookup;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
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
  public List<SamplePreview> query(int offset, int limit) {
    return samplePreviewRepository.findAll(new OffsetBasedRequest(offset, limit)).getContent();
  }

  @Override
  public List<SamplePreview> queryByExperimentId(ExperimentId experimentId, int offset, int limit,
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
    return samplePreviewRepository.findSamplePreviewByExperimentId(experimentId,
        new OffsetBasedRequest(offset, limit, Sort.by(orders))).getContent();
  }

  @Override
  public int queryCount(int offset, int limit) {
    return samplePreviewRepository.findAll(new OffsetBasedRequest(offset, limit)).getContent()
        .size();
  }

  @Override
  public int queryCountByExperimentId(ExperimentId experimentId) {
    return samplePreviewRepository.findSamplePreviewByExperimentId(experimentId,
        Pageable.unpaged()).getContent().size();
  }

}
