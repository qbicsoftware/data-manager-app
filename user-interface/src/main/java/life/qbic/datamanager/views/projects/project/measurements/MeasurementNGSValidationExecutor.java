package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Measurement NGS Validation Executor</b>
 * <p>
 * Implementation of the {@link MeasurementValidationExecutor} interface,
 * handling the validation of to be registered or edited  {@link life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata}
 *
 */
@Component
public class MeasurementNGSValidationExecutor implements
    MeasurementValidationExecutor<NGSMeasurementMetadata> {

  private final MeasurementValidationService measurementValidationService;

  @Autowired
  public MeasurementNGSValidationExecutor(
      MeasurementValidationService measurementValidationService) {
    this.measurementValidationService = Objects.requireNonNull(measurementValidationService);
  }
  @Override
  public CompletableFuture<ValidationResult> validateRegistration(NGSMeasurementMetadata metadata,
      ProjectId projectId) {
    return CompletableFuture.completedFuture(
        measurementValidationService.validateNGS(metadata, projectId));
  }
  @Override
  public CompletableFuture<ValidationResult> validateUpdate(NGSMeasurementMetadata metadata,
      ProjectId projectId) {
    return measurementValidationService.validateNGSUpdate(metadata, projectId);
  }
}
