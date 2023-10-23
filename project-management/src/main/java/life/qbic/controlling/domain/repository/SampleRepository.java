package life.qbic.controlling.domain.repository;

import java.util.Collection;
import life.qbic.application.commons.Result;
import life.qbic.controlling.application.sample.SampleInformationService;
import life.qbic.controlling.domain.model.experiment.ExperimentId;
import life.qbic.controlling.domain.model.project.Project;
import life.qbic.controlling.domain.model.sample.Sample;
import life.qbic.controlling.domain.service.SampleDomainService.ResponseCode;

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
   * @param project the project this sample belongs to
   * @param samples a batch of samples to save
   * @since 1.0.0
   */Result<Collection<Sample>, ResponseCode> addAll(Project project, Collection<Sample> samples);

  Result<Collection<Sample>, SampleInformationService.ResponseCode> findSamplesByExperimentId(
      ExperimentId experimentId);
}
