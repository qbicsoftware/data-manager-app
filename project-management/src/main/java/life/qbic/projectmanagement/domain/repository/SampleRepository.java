package life.qbic.projectmanagement.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.service.SampleDomainService.ResponseCode;

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
   */
  Result<Collection<Sample>, ResponseCode> addAll(Project project, Collection<Sample> samples);

  Result<Collection<Sample>, ResponseCode> addAll(ProjectId projectId, Collection<Sample> samples);

  void deleteAll(Project project, Collection<SampleId> sampleIds);

  Result<Collection<Sample>, SampleInformationService.ResponseCode> findSamplesByExperimentId(
      ExperimentId experimentId);

  List<Sample> findSamplesByBatchId(BatchId batchId);

  void updateAll(Project project, Collection<Sample> updatedSamples);

  void updateAll(ProjectId projectId, Collection<Sample> updatedSamples);

  List<Sample> findSamplesBySampleId(List<SampleId> sampleId);

  Optional<Sample> findSample(SampleCode sampleCode);

  Optional<Sample> findSample(SampleId sampleId);

  boolean isSampleRemovable(SampleId sampleId);

  long countSamplesWithExperimentId(ExperimentId experimentId);

  class SampleRepositoryException extends RuntimeException {
    public SampleRepositoryException(String message) {
      super(message);
    }
  }

}
