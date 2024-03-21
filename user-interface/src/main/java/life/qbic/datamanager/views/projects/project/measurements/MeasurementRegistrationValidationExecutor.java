package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationResult;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService.Domain;
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
public class MeasurementRegistrationValidationExecutor implements
    MeasurementValidationExecutor {

  private final MeasurementValidationService measurementValidationService;

  @Autowired
  public MeasurementRegistrationValidationExecutor(
      MeasurementValidationService measurementValidationService) {
    this.measurementValidationService = Objects.requireNonNull(measurementValidationService);
  }

  @Override
  public MeasurementValidationResult validateNGS(NGSMeasurementMetadata metadata) {
    return measurementValidationService.validateNGS(metadata);
  }

  @Override
  public MeasurementValidationResult validateProteomics(ProteomicsMeasurementMetadata metadata) {
    return measurementValidationService.validateProteomics(metadata);
  }

  @Override
  public Optional<Domain> inferDomainByProperties(Collection<String> properties) {
    return measurementValidationService.inferDomainByPropertyTypes(properties);
  }
}
