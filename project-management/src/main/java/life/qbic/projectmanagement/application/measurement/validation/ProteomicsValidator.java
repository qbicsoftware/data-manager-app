package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
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
public class ProteomicsValidator implements Validator<ProteomicsMeasurementMetadata> {

  private static final Set<String> PROTEOMICS_PROPERTIES = new HashSet<>();

  static {
    PROTEOMICS_PROPERTIES.addAll(
        Arrays.asList("qbic sample ids", "organisation id", "facility", "instrument",
            "pooled sample label", "cycle/fraction name", "fractionation type", "digestion method",
            "digestion enzyme", "enrichment method", "injection volume (ul)", "lc column",
            "lcms method", "sample preparation", "sample cleanup (protein)",
            "sample cleanup (peptide)", "note"));
  }

  protected final SampleInformationService sampleInformationService;

  @Autowired
  public ProteomicsValidator(SampleInformationService sampleInformationService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

  /**
   * Given a collection of properties, the validator determines if they mach the expected properties
   * for a QBiC-defined proteomics measurement metadata object.
   *
   * @param properties
   * @return
   * @since 1.0.0
   */
  public static boolean isProteomics(Collection<String> properties) {
    if (properties.isEmpty()) {
      return false;
    }
    if (properties.size() != PROTEOMICS_PROPERTIES.size()) {
      return false;
    }
    for (String pxpProperty : PROTEOMICS_PROPERTIES) {
      var propertyFound = properties.stream()
          .filter(property -> Objects.equals(property.toLowerCase(), pxpProperty)).findAny();
      if (propertyFound.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static Collection<String> properties() {
    return PROTEOMICS_PROPERTIES.stream().toList();
  }

  @Override
  public ValidationResult validate(ProteomicsMeasurementMetadata measurementMetadata) {
    // TODO implement property validation
    var validationPolicy = new ValidationPolicy();
    return validationPolicy.validateSampleIds(measurementMetadata.sampleCodes())
        .combine(validationPolicy.validateOrganisation(measurementMetadata.organisationId()));
  }


  private class ValidationPolicy {

    private final String UNKNOWN_SAMPLE_MESSAGE = "Unknown sample with sample id \"%s\"";

    private final String UNKNOWN_ORGANISATION_ID_MESSAGE = "The organisation ID does not seem to be a ROR ID: \"%s\"";

    // The unique ROR id part of the URL is described in the official documentation:
    // https://ror.readme.io/docs/ror-identifier-pattern
    private final String ROR_ID_REGEX = "^https://ror.org/0[a-z|0-9]{6}[0-9]{2}$";

    ValidationResult validateSampleIds(Collection<SampleCode> sampleCodes) {
      if (sampleCodes.isEmpty()) {
        return ValidationResult.withFailures(1,
            List.of("A measurement must contain at least one sample reference. Provided: none"));
      }
      ValidationResult validationResult = ValidationResult.successful(0);
      for (SampleCode sample : sampleCodes) {
        validationResult = validationResult.combine(validateSampleId(sample));
      }
      return validationResult;
    }

    ValidationResult validateSampleId(SampleCode sampleCodes) {
      var queriedSampleEntry = sampleInformationService.findSampleId(sampleCodes);
      if (queriedSampleEntry.isPresent()) {
        return ValidationResult.successful(1);
      }
      return ValidationResult.withFailures(1,
          List.of(UNKNOWN_SAMPLE_MESSAGE.formatted(sampleCodes.code())));
    }

    ValidationResult validateOrganisation(String organisationId) {
      if (Pattern.compile(ROR_ID_REGEX).matcher(organisationId).find()) {
        return ValidationResult.successful(1);
      }
      return ValidationResult.withFailures(1,
          List.of(UNKNOWN_ORGANISATION_ID_MESSAGE.formatted(organisationId)));
    }

  }
}
