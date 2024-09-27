package life.qbic.projectmanagement.application.sample;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
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
  public ValidationResultWithPayload<SampleMetadata> validateNewSample(String condition,
      String species, String specimen,
      String analyte, String analysisMethod, String experimentId,
      String projectId) {
    return sampleValidation.validateNewSample(condition, species, specimen, analyte, analysisMethod,
        experimentId, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public ValidationResultWithPayload<SampleMetadata> validateExistingSample(String sampleId,
      String condition, String species,
      String specimen, String analyte, String analysisMethod, String experimentId,
      String projectId) {
    return sampleValidation.validateExistingSample(sampleId, condition, species, specimen, analyte,
        analysisMethod, experimentId, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  @Async
  public CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validateNewSampleAsync(
      String condition,
      String species, String specimen,
      String analyte, String analysisMethod, String experimentId,
      String projectId) {
    return CompletableFuture.completedFuture(
        validateNewSample(condition, species, specimen, analyte, analysisMethod,
            experimentId, projectId));
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  @Async
  public CompletableFuture<ValidationResultWithPayload<SampleMetadata>> validateExistingSampleAsync(
      String sampleId, String condition, String species,
      String specimen, String analyte, String analysisMethod, String experimentId,
      String projectId) {
    return CompletableFuture.completedFuture(
        validateExistingSample(sampleId, condition, species, specimen, analyte,
            analysisMethod, experimentId, projectId));
  }

}
