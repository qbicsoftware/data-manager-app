package life.qbic.projectmanagement.application.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SampleRegistrationServiceV2 {

  private final BatchRegistrationService batchRegistrationService;
  private final SampleRepository sampleRepository;
  private final SampleCodeService sampleCodeService;
  private final ExperimentInformationService experimentInformationService;

  public SampleRegistrationServiceV2(BatchRegistrationService batchRegistrationService,
      SampleRepository sampleRepository, SampleCodeService sampleCodeService,
      ExperimentInformationService experimentInformationService) {
    this.batchRegistrationService = Objects.requireNonNull(batchRegistrationService);
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
    this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public CompletableFuture<Void> registerNewSamples(Collection<SampleMetadata> sampleRegistrationRequests,
      ProjectId projectId, String batchLabel, boolean batchIsPilot)
      throws RegistrationException {
    var result = batchRegistrationService.registerBatch(batchLabel, batchIsPilot, projectId);
    if (result.isError()) {
      throw new RegistrationException("Batch registration failed");
    }
    var batchId = result.getValue();

    throw new UnsupportedOperationException("Not implemented yet");
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public CompletableFuture<Void> updateSamples(Collection<SampleMetadata> sampleRegistrationRequests,
      ExperimentId experimentId, ProjectId projectId) throws RegistrationException {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private void registerSamples(Collection<SampleMetadata> sampleMetadata, BatchId batchId, ExperimentId experimentId, ProjectId projectId)
      throws RegistrationException {
    var experimentQuery = experimentInformationService.find(projectId.value(), experimentId);
    if (experimentQuery.isEmpty()) {
      throw new RegistrationException("Experiment not found");
    }
    var registeredSamples = new ArrayList<SampleId>();
    try {
      //sampleRepository.addAll()
    } catch (Exception e) {
      rollbackSampleRegistration(registeredSamples);
    }
  }

  private void rollbackSampleRegistration(Collection<SampleId> registeredSamples) {

  }

  public static class RegistrationException extends RuntimeException {
    public RegistrationException(String message) {
      super(message);
    }
  }

}
