package life.qbic.projectmanagement.application.measurement.validation;

import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Measurement Validator</b>
 *
 * <p>Validation interface employed to check the provided user input for a measurement.
 *    Implementations of this interface are supposed to be domain specific and will
 *    return a ValidationResult dependent on the presence or absence of data
 * </p>
 *
 */
public interface MeasurementValidator<T extends MeasurementMetadata> {

  ValidationResult validate(T measurementMetadata, ProjectId projectId);

}
