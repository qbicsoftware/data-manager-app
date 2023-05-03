package life.qbic.projectmanagement.domain.project.repository;

import life.qbic.projectmanagement.domain.project.sample.Batch;

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
   * @since 1.0.0
   */
  void add(Batch batch);

}
