package life.qbic.datamanager.views.projects.project.measurements;

import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;

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
public interface MeasurementValidationExecutor<MeasurementMetadata> {

  ValidationResult validateRegistration(MeasurementMetadata metadata);

  ValidationResult validateEdit(MeasurementMetadata metadata);

}