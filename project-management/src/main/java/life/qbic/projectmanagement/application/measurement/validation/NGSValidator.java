package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Collection;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class NGSValidator implements Validator<NGSMeasurement> {

  @Override
  public ValidationResult validate(NGSMeasurement measurementMetadata) {
    // TODO implement property validation
    return new ValidationResult();
  }

  public boolean isNGS(Collection<String> properties) {
    return false;
  }
}
