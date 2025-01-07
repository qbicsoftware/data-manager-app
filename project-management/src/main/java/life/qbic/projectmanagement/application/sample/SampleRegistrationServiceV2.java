package life.qbic.projectmanagement.application.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.repository.BatchRepository;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class SampleRegistrationServiceV2 {

  private final BatchRegistrationService batchRegistrationService;
  private final SampleRepository sampleRepository;
  private final BatchRepository batchRepository;
  private final SampleCodeService sampleCodeService;
  private final DeletionService deletionService;

  @Autowired
  public SampleRegistrationServiceV2(BatchRegistrationService batchRegistrationService,
      SampleRepository sampleRepository, BatchRepository batchRepository,
      SampleCodeService sampleCodeService,
      DeletionService deletionService) {
    this.batchRegistrationService = Objects.requireNonNull(batchRegistrationService);
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
    this.batchRepository = Objects.requireNonNull(batchRepository);
    this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
    this.deletionService = Objects.requireNonNull(deletionService);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Async
  public CompletableFuture<Void> registerSamples(Collection<SampleMetadata> sampleMetadata,
      ProjectId projectId, String batchLabel, boolean batchIsPilot, ExperimentId experimentId)
      throws RegistrationException {
    var result = batchRegistrationService.registerBatch(batchLabel, batchIsPilot, projectId,
        experimentId);
    if (result.isError()) {
      throw new RegistrationException("Batch registration failed");
    }
    var batchId = result.getValue();
    try {
      var sampleIds = registerSamples(sampleMetadata, batchId, projectId);
      batchRegistrationService.addSamplesToBatch(sampleIds, batchId, projectId);
    } catch (RuntimeException e) {
      rollbackSampleRegistration(batchId);
      deletionService.deleteSamples(projectId, batchId,
          sampleMetadata.stream().map(SampleMetadata::sampleId).toList());
      deletionService.deleteBatch(projectId, batchId);
      throw e;
    }
    return CompletableFuture.completedFuture(null);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Async
  public CompletableFuture<Void> updateSamples(
      Collection<SampleMetadata> sampleMetadata,
      ProjectId projectId,
      BatchId batchId,
      String batchLabel,
      boolean isPilot)
      throws RegistrationException {

    Batch batch = batchRepository.find(batchId)
        .orElseThrow(() -> new RegistrationException("Batch not found."));
    batch.setLabel(batchLabel);
    batch.setPilot(isPilot);

    var sampleIds = sampleMetadata.stream()
        .map(SampleMetadata::sampleId)
        .toList();
    var samples = sampleRepository.findSamplesBySampleId(sampleIds);
    var sampleBySampleCode = samples.stream()
        .collect(Collectors.toMap(sample -> sample.sampleCode().code(), Function.identity()));
    var updatedSamples = updateSamples(sampleBySampleCode, sampleMetadata);
    sampleRepository.updateAll(projectId, updatedSamples);
    batchRepository.update(batch);
    return CompletableFuture.completedFuture(null);
  }

  private List<Sample> updateSamples(Map<String, Sample> samples,
      Collection<SampleMetadata> sampleMetadataList) {
    var updatedSamples = new ArrayList<Sample>();
    for (SampleMetadata sampleMetadata : sampleMetadataList) {
      var sampleForUpdate = samples.get(sampleMetadata.sampleCode());
      sampleForUpdate.setLabel(sampleMetadata.sampleName());
      sampleForUpdate.setAnalysisMethod(sampleMetadata.analysisToBePerformed());
      var sampleOrigin = SampleOrigin.create(sampleMetadata.species(), sampleMetadata.specimen(),
          sampleMetadata.analyte());
      sampleForUpdate.setSampleOrigin(sampleOrigin);
      sampleForUpdate.setBiologicalReplicate(sampleMetadata.biologicalReplicate());
      sampleForUpdate.setExperimentalGroupId(sampleMetadata.experimentalGroupId());
      sampleForUpdate.setComment(sampleMetadata.comment());
      updatedSamples.add(sampleForUpdate);
    }
    return updatedSamples;
  }

  private Collection<SampleId> registerSamples(Collection<SampleMetadata> sampleMetadata,
      BatchId batchId,
      ProjectId projectId)
      throws RegistrationException {
    var samplesToRegister = new ArrayList<Sample>();
    var sampleCodes = generateSampleCodes(sampleMetadata.size(), projectId).iterator();
    for (SampleMetadata sample : sampleMetadata) {
      samplesToRegister.add(buildSample(sample, batchId, sampleCodes.next()));
    }
    return sampleRepository.addAll(projectId, samplesToRegister)
        .valueOrElseThrow(e -> new RegistrationException("Could not register samples: " + e.name()))
        .stream().map(Sample::sampleId)
        .toList();
  }

  private Sample buildSample(SampleMetadata sample, BatchId batchId, SampleCode sampleCode) {
    var sampleOrigin = SampleOrigin.create(sample.species(), sample.specimen(), sample.analyte());
    return Sample.create(sampleCode,
        new SampleRegistrationRequest(sample.sampleName(), sample.biologicalReplicate(), batchId,
            ExperimentId.parse(sample.experimentId()), sample.experimentalGroupId(), sampleOrigin,
            sample.analysisToBePerformed(), sample.comment()));
  }

  private List<SampleCode> generateSampleCodes(int amount, ProjectId projectId) {
    var codes = new ArrayList<SampleCode>();
    for (int i = 0; i < amount; i++) {
      codes.add(sampleCodeService.generateFor(projectId).getValue());
    }
    return codes;
  }

  private void rollbackSampleRegistration(BatchId batchId) {
    batchRegistrationService.deleteBatch(batchId);
  }

  public static class RegistrationException extends RuntimeException {

    public RegistrationException(String message) {
      super(message);
    }
  }

  public static class UnknownSampleException extends RuntimeException {

    public UnknownSampleException(String message) {
      super(message);
    }
  }

}
