package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class NGSValidator implements Validator<NGSMeasurement> {

  private static final Set<String> NGS_PROPERTIES = new HashSet<>();

  static {
    NGS_PROPERTIES.addAll(Arrays.asList("qbic sample id", "organism id", "facility", "instrument",
        "sequencing read type", "library kit", "flow cell", "run protocol", "index i5", "index i7",
        "note"));
  }

  @Override
  public ValidationResult validate(NGSMeasurement measurementMetadata) {
    // TODO implement property validation
    return new ValidationResult();
  }

  public boolean isNGS(Collection<String> properties) {
    if (properties.isEmpty()) {
      return false;
    }
    if (properties.size() != NGS_PROPERTIES.size()) {
      return false;
    }
    for (String ngsProperty : NGS_PROPERTIES) {
      var propertyFound = properties.stream().filter(property -> Objects.equals(property, ngsProperty)).findAny();
      if (propertyFound.isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
