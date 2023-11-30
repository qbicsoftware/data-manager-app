package life.qbic.projectmanagement.application.batch;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic batch information
 *
 * @since 1.0.0
 */
@Service
public class BatchInformationService {

  private static final Logger log = LoggerFactory.logger(BatchInformationService.class);
  private final BatchRepository batchRepository;

  public BatchInformationService(@Autowired BatchRepository batchRepository) {
    this.batchRepository = batchRepository;
  }

  public Optional<Batch> find(BatchId batchId) {
    Objects.requireNonNull(batchId);
    log.debug("Search for batch with id: " + batchId.value());
    return batchRepository.find(batchId);
  }

  public Result<Collection<Batch>, ResponseCode> retrieveBatchesForExperiment(
      ExperimentId experimentId) {
    Objects.requireNonNull(experimentId, "Experiment id must not be null");
    var result = batchRepository.findBatchesByExperimentId(experimentId);
    if (result.isError()) {
      return Result.fromError(ResponseCode.BATCHES_NOT_FOUND);
    } else {
      return Result.fromValue(result.getValue());
    }
  }

  public enum ResponseCode {
    BATCHES_NOT_FOUND
  }
}
