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
 * <b>Measurement Edit Validation Executor</b>
 * <p>
 * Implementation of the {@link MeasurementValidationExecutor} interface,
 * handling the validation of to be edited {@link life.qbic.projectmanagement.application.measurement.MeasurementMetadata}
 *
 */
@Component
public class MeasurementEditValidationExecutor implements MeasurementValidationExecutor {

  private final MeasurementValidationService measurementValidationService;

  @Autowired
  public MeasurementEditValidationExecutor(
      MeasurementValidationService measurementValidationService) {
    this.measurementValidationService = Objects.requireNonNull(measurementValidationService);
  }

  @Override
  public MeasurementValidationResult validateNGS(NGSMeasurementMetadata metadata) {
    // TODO call the edit validation method once we have them
    return measurementValidationService.validateNGS(metadata);
  }

  @Override
  public MeasurementValidationResult validateProteomics(ProteomicsMeasurementMetadata metadata) {
    // TODO call the edit validation method once we have them
    return measurementValidationService.validateProteomics(metadata);
  }

  @Override
  public Optional<Domain> inferDomainByProperties(Collection<String> properties) {
    // TODO call the edit infer domain method once we have it
    return measurementValidationService.inferDomainByPropertyTypes(properties);
  }


}
