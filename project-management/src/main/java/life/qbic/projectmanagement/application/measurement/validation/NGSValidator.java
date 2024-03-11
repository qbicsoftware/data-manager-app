package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class NGSValidator implements Validator<NGSMeasurementMetadata> {

  private static final Set<String> NGS_PROPERTIES = new HashSet<>();

  private final SampleInformationService sampleInformationService;

  static {
    NGS_PROPERTIES.addAll(
        Arrays.asList("qbic sample id", "organisation id", "facility", "instrument",
            "sequencing read type", "library kit", "flow cell", "run protocol", "index i5",
            "index i7",
            "note"));
  }
  @Autowired
  public NGSValidator(SampleInformationService sampleInformationService) {
    this.sampleInformationService = sampleInformationService;
  }

  /**
   * Given a collection of properties, the validator determines if they mach the expected properties
   * for a QBiC-defined NGS measurement metadata object.
   *
   * @param properties
   * @return
   * @since 1.0.0
   */
  public static boolean isNGS(Collection<String> properties) {
    if (properties.isEmpty()) {
      return false;
    }
    if (properties.size() != NGS_PROPERTIES.size()) {
      return false;
    }
    for (String ngsProperty : NGS_PROPERTIES) {
      var propertyFound = properties.stream()
          .filter(property -> Objects.equals(property.toLowerCase(), ngsProperty)).findAny();
      if (propertyFound.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static Collection<String> properties() {
    return NGS_PROPERTIES.stream().toList();
  }

  @Override
  public ValidationResult validate(NGSMeasurementMetadata measurementMetadata) {
    // TODO implement property validation
    return ValidationResult.withFailures(1, List.of("This went wrong"));
  }
}
