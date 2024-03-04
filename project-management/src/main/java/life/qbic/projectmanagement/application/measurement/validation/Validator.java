package life.qbic.projectmanagement.application.measurement.validation;

import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface Validator<T extends MeasurementMetadata> {

  ValidationResult validate(T measurementMetadata);

}
