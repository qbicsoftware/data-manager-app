package life.qbic.projectmanagement.domain.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.model.sample.event.SampleDeleted;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Sample Domain Service</b>
 * <p>
 * Service that handles {@link Sample} creation and deletion events, that need to dispatch domain
 * events.
 *
 * @since 1.0.0
 */
@Service
public class SampleDomainService {

  private final SampleRepository sampleRepository;

  @Autowired
  public SampleDomainService(SampleRepository sampleRepository) {
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
  }

  public Result<Collection<Sample>, ResponseCode> registerSamples(Project project,
      Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests) {
    Objects.requireNonNull(sampleCodesToRegistrationRequests);
    Collection<Sample> samplesToRegister = new ArrayList<>();
    sampleCodesToRegistrationRequests.forEach((sampleCode, sampleRegistrationRequest) -> {
      var sample = Sample.create(sampleCode, sampleRegistrationRequest);
      samplesToRegister.add(sample);
    });
    Result<Collection<Sample>, ResponseCode> result = this.sampleRepository.addAll(project,
        samplesToRegister);
    result.onValue(
            createdSamples -> createdSamples.forEach(s -> dispatchSuccessfulSampleRegistration(s)))
        .onError(Result::fromError);
    if(result.isValue()) {
      dispatchProjectChanged(project.getId());
    }
    return result;
  }

  public void updateSamples(Project project, Collection<SampleUpdateRequest> updatedSamples) {
    Objects.requireNonNull(updatedSamples);
    List<SampleId> sampleIds = updatedSamples.stream().map(SampleUpdateRequest::sampleId).toList();
    Collection<Sample> samplesToUpdate = sampleRepository.findSamplesBySampleId(sampleIds);
    for (Sample sample : samplesToUpdate) {
      var sampleInfo = updatedSamples.stream()
          .filter(sampleUpdateRequest -> sampleUpdateRequest.sampleId().equals(sample.sampleId()))
          .findFirst().orElseThrow();
      sample.setLabel(sampleInfo.sampleInformation().sampleLabel());
      sample.setOrganismId(sampleInfo.sampleInformation().organismId());
      sample.setAnalysisMethod(sampleInfo.sampleInformation().analysisMethod());
      sample.setSampleOrigin(SampleOrigin.create(sampleInfo.sampleInformation().species(),
          sampleInfo.sampleInformation().specimen(), sampleInfo.sampleInformation().analyte()));
      sample.setComment(sampleInfo.sampleInformation().comment());
      sample.setBiologicalReplicateId(sampleInfo.sampleInformation().biologicalReplicate().id());
      sample.setExperimentalGroupId(sampleInfo.sampleInformation().experimentalGroup().id());
    }
    sampleRepository.updateAll(project, samplesToUpdate);
    dispatchSuccessfulSampleUpdate(project.getId());
  }

  private void dispatchSuccessfulSampleUpdate(ProjectId projectID) {
    ProjectChanged projectChanged = ProjectChanged.create(projectID);
    DomainEventDispatcher.instance().dispatch(projectChanged);
  }

  public void deleteSamples(Project project, BatchId batchId, Collection<SampleId> samples) {
    Objects.requireNonNull(samples);
    sampleRepository.deleteAll(project, samples);
    samples.forEach(sampleId -> dispatchSuccessfulSampleDeletion(sampleId, batchId));
    if(!samples.isEmpty()) {
      dispatchProjectChanged(project.getId());
    }
  }

  private void dispatchSuccessfulSampleDeletion(SampleId sampleId, BatchId batchId) {
    SampleDeleted sampleDeleted = SampleDeleted.create(batchId, sampleId);
    DomainEventDispatcher.instance().dispatch(sampleDeleted);
  }

  private void dispatchProjectChanged(ProjectId projectId) {
    ProjectChanged projectChanged = ProjectChanged.create(projectId);
    DomainEventDispatcher.instance().dispatch(projectChanged);
  }

  private void dispatchSuccessfulSampleRegistration(Sample sample) {
    SampleRegistered sampleRegistered = SampleRegistered.create(sample.assignedBatch(),
        sample.sampleId());
    DomainEventDispatcher.instance().dispatch(sampleRegistered);
  }

  public boolean isSampleRemovable(SampleId sampleId) {
    return sampleRepository.isSampleRemovable(sampleId);
  }

  /**
   * Response error codes for the sample registration
   *
   * @since 1.0.0
   */
  public enum ResponseCode {
    REGISTRATION_FAILED, DELETION_FAILED, DATA_ATTACHED_TO_SAMPLES, UPDATE_FAILED
  }

}
