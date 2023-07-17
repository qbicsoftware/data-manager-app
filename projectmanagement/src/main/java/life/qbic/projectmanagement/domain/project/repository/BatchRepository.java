package life.qbic.projectmanagement.domain.project.repository;

import java.util.Collection;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.service.BatchDomainService.ResponseCode;

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

  /**
   * Deletes a {@link Batch} entity persistently.
   *
   * @param batchId the {@link BatchId} of the to be deleted sample batch
   * @return a {@link Result} with the BatchId as value or an error response code
   * {@link ResponseCode}.
   */
  Result<BatchId, ResponseCode> deleteById(BatchId batchId);

  Result<Collection<Batch>, life.qbic.projectmanagement.application.batch.BatchInformationService.ResponseCode> findBatchesByExperimentId(
      ExperimentId experimentId);

}
