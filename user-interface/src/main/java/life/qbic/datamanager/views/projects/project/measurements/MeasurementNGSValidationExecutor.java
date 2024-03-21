package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Objects;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationResult;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Registration Validation Executor</b>
 * <p>
 * Implementation of the {@link MeasurementValidationExecutor} interface,
 * handling the validation of newly registered {@link life.qbic.projectmanagement.application.measurement.MeasurementMetadata}
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
  public MeasurementValidationResult validateRegistration(NGSMeasurementMetadata metadata) {
    return measurementValidationService.validateNGS(metadata);
  }
  @Override
  public MeasurementValidationResult validateEdit(NGSMeasurementMetadata metadata) {
    //Todo provide edit validation for ngs;
    return null;
  }
}
