package life.qbic.projectmanagement.application.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;

/**
 * SampleInformationService
 * <p>
 * Service that provides an API to query sample information
 */
@Service
public class SampleInformationService {

  private static final Logger log = LoggerFactory.logger(SampleInformationService.class);
  private final SampleRepository sampleRepository;
  private final SamplePreviewLookup samplePreviewLookup;

  public SampleInformationService(@Autowired SamplePreviewLookup samplePreviewLookup,
      @Autowired SampleRepository sampleRepository) {
    Objects.requireNonNull(samplePreviewLookup);
    Objects.requireNonNull(sampleRepository);
    this.samplePreviewLookup = samplePreviewLookup;
    this.sampleRepository = sampleRepository;
  }

  public Result<Collection<Sample>, ResponseCode> retrieveSamplesForExperiment(
      ExperimentId experimentId) {
    Objects.requireNonNull(experimentId, "experiment id must not be null");
    return sampleRepository.findSamplesByExperimentId(experimentId);
  }

  /**
   * Queries {@link SamplePreview}s with a provided offset and limit that supports pagination.
   *
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   * @since 1.0.0
   */
  @PostFilter("hasPermission(filterObject.sampleId(),'life.qbic.projectmanagement.domain.project.sample.Sample','READ')")
  public List<SamplePreview> queryPreview(ExperimentId experimentId, int offset, int limit,
      List<SortOrder> sortOrders, String filter) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<SamplePreview> previewList = samplePreviewLookup.queryByExperimentId(experimentId,
        offset,
        limit,
        sortOrders, filter);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(previewList);
  }

  public int countPreviews(ExperimentId experimentId, String filter) {
    // returned by JPA -> UnmodifiableRandomAccessList
    return samplePreviewLookup.queryCountByExperimentId(experimentId, filter);
  }

  public enum ResponseCode {
    QUERY_FAILED
  }
}
