package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
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

  protected final SampleInformationService sampleInformationService;

  protected final OntologyLookupService ontologyLookupService;

  @Autowired
  public ProteomicsValidator(SampleInformationService sampleInformationService,
      OntologyLookupService ontologyLookupService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.ontologyLookupService = Objects.requireNonNull(ontologyLookupService);
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
    if (properties.size() < PROTEOMICS_PROPERTY.values().length) {
      return false;
    }
    for (PROTEOMICS_PROPERTY pxpProperty : PROTEOMICS_PROPERTY.values()) {
      var propertyFound = properties.stream()
          .filter(property -> Objects.equals(property.toLowerCase(), pxpProperty.label()))
          .findAny();
      if (propertyFound.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public static Collection<String> properties() {
    return Arrays.stream(PROTEOMICS_PROPERTY.values()).map(PROTEOMICS_PROPERTY::label).toList();
  }

  @Override
  public ValidationResult validate(ProteomicsMeasurementMetadata measurementMetadata) {
    var validationPolicy = new ValidationPolicy();
    return validationPolicy.validateSampleIds(measurementMetadata.sampleCodes())
        .combine(validationPolicy.validateOrganisation(measurementMetadata.organisationId())
            .combine(validationPolicy.validateInstrument(measurementMetadata.instrumentCURI())))
        .combine(validationPolicy.validateSamplePoolGroup(measurementMetadata.samplePoolGroup()));
  }

  public enum PROTEOMICS_PROPERTY {
    QBIC_SAMPLE_ID("qbic sample ids"),
    SAMPLE_LABEL("sample label"),
    ORGANISATION_ID("organisation id"),
    FACILITY("facility"),
    INSTRUMENT("instrument"),
    SAMPLE_POOL_GROUP("sample pool group"),
    CYCLE_FRACTION_NAME("cycle/fraction name"),
    DIGESTION_METHOD("digestion method"),
    DIGESTION_ENZYME("digestion enzyme"),
    ENRICHMENT_METHOD("enrichment method"),
    INJECTION_VOLUME("injection volume (ul)"),
    LC_COLUMN("lc column"),
    LCMS_METHOD("lcms method"),
    SAMPLE_PREPARATION("sample preparation"),
    SAMPLE_CLEANUP_PROTEIN("sample cleanup (protein)"),
    SAMPLE_CLEANUP_PEPTIDE("sample cleanup (peptide)"),
    NOTE("note");

    private final String label;


    PROTEOMICS_PROPERTY(String propertyLabel) {
      this.label = propertyLabel;
    }

    public String label() {
      return this.label;
    }

  }

  private class ValidationPolicy {

    private final static String UNKNOWN_SAMPLE_MESSAGE = "Unknown sample with sample id \"%s\"";

    private final static String UNKNOWN_ORGANISATION_ID_MESSAGE = "The organisation ID does not seem to be a ROR ID: \"%s\"";

    private final static String UNKNOWN_INSTRUMENT_ID = "Unknown instrument id: \"%s\"";

    // The unique ROR id part of the URL is described in the official documentation:
    // https://ror.readme.io/docs/ror-identifier-pattern
    private final static String ROR_ID_REGEX = "^https://ror.org/0[a-z|0-9]{6}[0-9]{2}$";

    private final static String NOT_A_NUMBER_SAMPLE_POOL = "Sample pool group was not a number: \"%s\"";

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

    ValidationResult validateInstrument(String instrument) {
      var result = ontologyLookupService.findByCURI(instrument);
      if (result.isPresent()) {
        return ValidationResult.successful(1);
      }
      return ValidationResult.withFailures(1, List.of(UNKNOWN_INSTRUMENT_ID.formatted(instrument)));
    }

    ValidationResult validateSamplePoolGroup(String samplePoolGroup) {
      if (samplePoolGroup.isBlank()) {
        return ValidationResult.successful(1);
      }
      try {
        Integer.parseInt(samplePoolGroup);
      } catch (NumberFormatException e) {
        return ValidationResult.withFailures(1,
            List.of(NOT_A_NUMBER_SAMPLE_POOL.formatted(samplePoolGroup)));
      }
      return ValidationResult.successful(1);
    }

  }
}
