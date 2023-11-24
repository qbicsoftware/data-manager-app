package life.qbic.projectmanagement.domain.repository;

import java.util.Collection;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService.ResponseCode;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * Batch data storage interface
 * <p>
 * Provides access to the persistence layer that handles the {@link Batch} data storage.
 *
 * @since 1.0.0
 */
public interface BatchRepository {

  /**
   * Saves a {@link Batch} entity persistently.
   *
   * @param batch the sample batch to register
   * @return a {@link Result} with the batch as value or an error response code {@link ResponseCode}.
   * @since 1.0.0
   */
  Result<Batch, ResponseCode> add(Batch batch);

  Optional<Batch> find(BatchId batchId);

  Result<Batch, ResponseCode> update(Batch batch);

  Result<Collection<Batch>, ResponseCode> findBatchesByExperimentId(
      ExperimentId experimentId);

}
