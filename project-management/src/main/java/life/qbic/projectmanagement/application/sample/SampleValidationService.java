package life.qbic.projectmanagement.application.sample;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
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
  public ValidationResult validateNewSample(SampleMetadata sampleMetadata, String experimentId,
      String projectId) {
    return sampleValidation.validateNewSample(sampleMetadata, experimentId, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public ValidationResult validateExistingSample(SampleMetadata sampleMetadata, String experimentId,
      String projectId) {
    return sampleValidation.validateExistingSample(sampleMetadata, experimentId, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public CompletableFuture<ValidationResult> validateNewSampleAsync(SampleMetadata sampleMetadata,
      String experimentId, String projectId) {
    return CompletableFuture.completedFuture(
        validateNewSample(sampleMetadata, experimentId, projectId));
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public CompletableFuture<ValidationResult> validateExistingSampleAsync(
      SampleMetadata sampleMetadata, String experimentId, String projectId) {
    return CompletableFuture.completedFuture(
        validateExistingSample(sampleMetadata, experimentId, projectId));
  }

}
