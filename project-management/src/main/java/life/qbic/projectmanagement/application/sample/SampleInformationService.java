package life.qbic.projectmanagement.application.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SampleInformationService
 * <p>
 * Service that provides an API to search sample information
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

  /**
   * Checks if there are samples registered for the provided experimentId
   *
   * @param experimentId {@link ExperimentId}s of the experiment for which it should be determined
   *                     if it has samples registered
   * @return true if experiments has samples, false if not
   */
  public boolean hasSamples(ExperimentId experimentId) {
    Objects.requireNonNull(experimentId, "experiment id must not be null");
    return sampleRepository.countSamplesWithExperimentId(experimentId) != 0;
  }

  public Result<Collection<Sample>, ResponseCode> retrieveSamplesForExperiment(
      ExperimentId experimentId) {
    Objects.requireNonNull(experimentId, "experiment id must not be null");
    try {
      return Result.fromValue(sampleRepository.findSamplesByExperimentId(experimentId));
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.fromError(ResponseCode.QUERY_FAILED);
    }
  }

  public List<SamplePreview> retrieveSamplePreviewsForExperiment(ExperimentId experimentId) {
    return samplePreviewLookup.queryByExperimentId(experimentId);
  }

  public List<Sample> retrieveSamplesByIds(Collection<SampleId> sampleIds) {
    return sampleRepository.findSamplesBySampleId(sampleIds.stream().toList());
  }

  public List<Sample> retrieveSamplesForBatch(BatchId batchId) {
    Objects.requireNonNull(batchId, "batch id must not be null");
    return sampleRepository.findSamplesByBatchId(batchId);
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

  public Optional<Sample> findSample(SampleId sampleId) {
    return sampleRepository.findSample(sampleId);
  }

  public Optional<SampleIdCodeEntry> findSampleId(SampleCode sampleCode) {
    return sampleRepository.findSample(sampleCode)
        .map(sample -> new SampleIdCodeEntry(sample.sampleId(), sampleCode));
  }

  public int countPreviews(ExperimentId experimentId, String filter) {
    // returned by JPA -> UnmodifiableRandomAccessList
    return samplePreviewLookup.queryCountByExperimentId(experimentId, filter);
  }

  public enum ResponseCode {
    QUERY_FAILED
  }
}
