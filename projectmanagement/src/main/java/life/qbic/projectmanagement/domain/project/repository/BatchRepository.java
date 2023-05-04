package life.qbic.projectmanagement.domain.project.repository;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.sample.Batch;
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

}
