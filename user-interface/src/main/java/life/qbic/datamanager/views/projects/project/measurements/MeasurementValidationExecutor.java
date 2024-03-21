package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Collection;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationResult;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService.Domain;

/**
 * <b>Validation Executor</b>
 * <p>
 * An interface that enables injection of different configurations of
 * {@link MeasurementValidationService} calls,
 * based on the use case (create or edit).
 * <p>
 * To support more domains, just create more methods here.
 *
 * @since 1.0.0
 */
public interface MeasurementValidationExecutor {

  MeasurementValidationResult validateNGS(NGSMeasurementMetadata metadata);

  MeasurementValidationResult validateProteomics(ProteomicsMeasurementMetadata metadata);

  Optional<Domain> inferDomainByProperties(Collection<String> properties);

}
