package life.qbic.projectmanagement.application.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.SampleReference;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
  private final ConfoundingVariableService confoundingVariableService;

  @Autowired
  public SampleRegistrationServiceV2(BatchRegistrationService batchRegistrationService,
      SampleRepository sampleRepository, BatchRepository batchRepository,
      SampleCodeService sampleCodeService,
      DeletionService deletionService, ConfoundingVariableService confoundingVariableService) {
    this.batchRegistrationService = Objects.requireNonNull(batchRegistrationService);
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
    this.batchRepository = Objects.requireNonNull(batchRepository);
    this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
    this.deletionService = Objects.requireNonNull(deletionService);
    this.confoundingVariableService = confoundingVariableService;
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Void> registerSamples(Collection<SampleMetadata> sampleMetadata,
      ProjectId projectId, String batchLabel, boolean batchIsPilot, ExperimentReference experiment)
      throws RegistrationException {
    var result = batchRegistrationService.registerBatch(batchLabel, batchIsPilot, projectId);
    if (result.isError()) {
      throw new RegistrationException("Batch registration failed");
    }
    var batchId = result.getValue();
    try {
      var registeredSamples = registerSamples(sampleMetadata, batchId, projectId);
      for (Entry<Sample, SampleMetadata> registeredSample : registeredSamples.entrySet()) {
        Map<VariableReference, String> levels = new HashMap<>();
        registeredSample.getValue().confoundingVariables().forEach((k, v) -> levels.put(k.id(), v));
        SampleReference sampleReference = new SampleReference(
            registeredSample.getKey().sampleId().value());
        confoundingVariableService.setVariableLevelsForSample(projectId.value(),
            experiment,
            sampleReference, levels);
      }
      Set<SampleId> sampleIds = registeredSamples.keySet().stream().map(Sample::sampleId).collect(
          Collectors.toSet());
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
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Void> updateSamples(
      Collection<SampleMetadata> sampleMetadata,
      ProjectId projectId,
      BatchId batchId,
      String batchLabel,
      boolean isPilot,
      ExperimentReference experiment)
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
    sampleRepository.updateAll(projectId, updatedSamples.keySet());

    for (Entry<Sample, SampleMetadata> updatedSample : updatedSamples.entrySet()) {
      Map<VariableReference, String> levels = new HashMap<>();

      updatedSample.getValue().confoundingVariables().forEach((k, v) -> levels.put(k.id(), v));
      SampleReference sampleReference = new SampleReference(
          updatedSample.getKey().sampleId().value());
      confoundingVariableService.setVariableLevelsForSample(projectId.value(), experiment,
          sampleReference, levels);
    }
    batchRegistrationService.addSamplesToBatch(sampleIds, batchId, projectId);
    batchRepository.update(batch);
    return CompletableFuture.completedFuture(null);
  }

  private Map<Sample, SampleMetadata> updateSamples(Map<String, Sample> samples,
      Collection<SampleMetadata> sampleMetadataList) {
    var samplesToUpdate = new HashMap<Sample, SampleMetadata>();
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
      samplesToUpdate.put(sampleForUpdate, SampleMetadata.createUpdate(
          sampleMetadata.sampleId(),
          sampleMetadata.sampleCode(),
          sampleMetadata.sampleName(),
          sampleMetadata.analysisToBePerformed(),
          sampleMetadata.biologicalReplicate(),
          sampleMetadata.experimentalGroupId(),
          sampleMetadata.species(),
          sampleMetadata.specimen(),
          sampleMetadata.analyte(),
          sampleMetadata.comment(),
          sampleMetadata.confoundingVariables(),
          sampleMetadata.experimentId()
      ));
    }
    return samplesToUpdate;
  }

  private Map<Sample, SampleMetadata> registerSamples(Collection<SampleMetadata> sampleMetadata,
      BatchId batchId,
      ProjectId projectId)
      throws RegistrationException {
    var samplesToRegister = new HashMap<Sample, SampleMetadata>();
    var sampleCodes = generateSampleCodes(sampleMetadata.size(), projectId).iterator();
    for (SampleMetadata metadata : sampleMetadata) {
      Sample sample = buildSample(metadata, batchId, sampleCodes.next());
      samplesToRegister.put(sample, metadata);
    }
    var registeredSamples = new HashMap<Sample, SampleMetadata>();
    List<Sample> addedSamples = sampleRepository.addAll(projectId, samplesToRegister.keySet())
        .stream().toList();
    for (Sample addedSample : addedSamples) {
      SampleMetadata metadata = samplesToRegister.get(addedSample);
      registeredSamples.put(addedSample,
          SampleMetadata.fromNewToExisting(addedSample.sampleId(), metadata));
    }
    return registeredSamples;
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

}
