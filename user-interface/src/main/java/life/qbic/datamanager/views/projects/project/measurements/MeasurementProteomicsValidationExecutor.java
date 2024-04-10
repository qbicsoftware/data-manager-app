package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Objects;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * <b>Measurement Proteomics Validation Executor</b>
 * <p>
 * Implementation of the {@link MeasurementValidationExecutor} interface,
 * handling the validation of to be registered or edited {@link life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata}
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
  public ValidationResult validateRegistration(ProteomicsMeasurementMetadata metadata) {
    return measurementValidationService.validateProteomics(metadata);
  }

  @Override
  public ValidationResult validateUpdate(ProteomicsMeasurementMetadata metadata) {
    return measurementValidationService.validateProteomicsUpdate(metadata);
  }
}
