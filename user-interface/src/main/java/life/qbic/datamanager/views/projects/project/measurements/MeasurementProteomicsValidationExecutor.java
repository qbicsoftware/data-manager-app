package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * <b>Measurement Proteomics Validation Executor</b>
 * <p>
 * Implementation of the {@link MeasurementValidationExecutor} interface,
 * handling the validation of to be registered or edited {@link ProteomicsMeasurementMetadata}
 *
 */
@Component
public class MeasurementProteomicsValidationExecutor implements
    MeasurementValidationExecutor<ProteomicsMeasurementMetadata> {

  private final MeasurementValidationService measurementValidationService;

  @Autowired
  public MeasurementProteomicsValidationExecutor(
      MeasurementValidationService measurementValidationService) {
    this.measurementValidationService = Objects.requireNonNull(measurementValidationService);
  }

  @Override
  public CompletableFuture<ValidationResult> validateRegistration(
      ProteomicsMeasurementMetadata metadata, ProjectId projectId) {
    return CompletableFuture.completedFuture(
        measurementValidationService.validateProteomics(metadata, projectId));
  }

  @Override
  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public CompletableFuture<ValidationResult> validateUpdate(ProteomicsMeasurementMetadata metadata,
      ProjectId projectId) {
    return measurementValidationService.validateProteomicsUpdate(metadata, projectId);
  }
}
