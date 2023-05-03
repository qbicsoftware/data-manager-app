package life.qbic.projectmanagement.domain.project.repository;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.service.SampleDomainService.ResponseCode;

/**
 * Sample data storage interface
 * <p>
 * Provides access to the persistence layer that handles {@link Sample} data storage.
 *
 * @since 1.0.0
 */
public interface SampleRepository {

  /**
   * Saves a sample entity persistently.
   *
   * @param sample the sample to save.
   * @since 1.0.0
   */
  Result<Void, ResponseCode> add(Sample sample);

}
