package life.qbic.projectmanagement.application.sample;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistration;
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
public class SampleValidationService {

  private final SampleValidation sampleValidation;

  @Autowired
  public SampleValidationService(SampleValidation sampleValidation) {
    this.sampleValidation = Objects.requireNonNull(sampleValidation);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public ValidationResultWithPayload<SampleMetadata> validateSample(
      SampleRegistration registration) {
    if (registration.sampleCode() == null || registration.sampleCode().isBlank()) {
      return validateNewSample(registration.sampleName(),
          registration.biologicalReplicate(),
          registration.condition(),
          registration.species(),
          registration.specimen(),
          registration.analyte(),
          registration.analysisMethod(),
          registration.comment(),
          registration.confoundingVariables(),
          registration.experimentId(),
          registration.projectId());
    }
    return validateExistingSample(
        registration.sampleCode(),
        registration.sampleName(),
        registration.biologicalReplicate(),
        registration.condition(),
        registration.species(),
        registration.specimen(),
        registration.analyte(),
        registration.analysisMethod(),
        registration.comment(),
        registration.confoundingVariables(),
        registration.experimentId(),
        registration.projectId());
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public ValidationResultWithPayload<SampleMetadata> validateNewSample(
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      Map<String, String> confoundingVariables,
      String experimentId,
      String projectId) {
    return sampleValidation.validateNewSample(sampleName,
        biologicalReplicate,
        condition,
        species,
        specimen,
        analyte,
        analysisMethod,
        comment,
        confoundingVariables,
        experimentId,
        projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public ValidationResultWithPayload<SampleMetadata> validateExistingSample(
      String sampleCode,
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      Map<String, String> confoundingVariables,
      String experimentId,
      String projectId) {
    return sampleValidation.validateExistingSample(sampleCode,
        sampleName,
        biologicalReplicate,
        condition,
        species,
        specimen,
        analyte,
        analysisMethod,
        comment,
        confoundingVariables,
        experimentId,
        projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  @Async
  public CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validateNewSampleAsync(
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      Map<String, String> confoundingVariables,
      String experimentId,
      String projectId) {
    return CompletableFuture.completedFuture(
        validateNewSample(sampleName,
            biologicalReplicate,
            condition,
            species,
            specimen,
            analyte,
            analysisMethod,
            comment,
            confoundingVariables,
            experimentId,
            projectId));
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  @Async
  public CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validateExistingSampleAsync(
      String sampleCode,
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      Map<String, String> confoundingVariables,
      String experimentId,
      String projectId) {
    return CompletableFuture.completedFuture(
        validateExistingSample(sampleCode,
            sampleName,
            biologicalReplicate,
            condition,
            species,
            specimen,
            analyte,
            analysisMethod,
            comment,
            confoundingVariables,
            experimentId,
            projectId));
  }

}
