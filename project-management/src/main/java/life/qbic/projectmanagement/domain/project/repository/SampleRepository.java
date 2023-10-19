package life.qbic.projectmanagement.domain.project.repository;

import java.util.Collection;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
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
   * @param project the project this sample belongs to
   * @param samples a batch of samples to save
   * @since 1.0.0
   */Result<Collection<Sample>, ResponseCode> addAll(Project project, Collection<Sample> samples);

  Result<Collection<Sample>, SampleInformationService.ResponseCode> findSamplesByExperimentId(
      ExperimentId experimentId);
}
