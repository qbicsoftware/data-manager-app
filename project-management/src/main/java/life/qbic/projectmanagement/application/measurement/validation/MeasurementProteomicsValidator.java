package life.qbic.projectmanagement.application.measurement.validation;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * <b>Measurement Proteomics Validator</b>
 *
 * <p>Validator employed to check the provided user input for a measurement in the proteomics
 * domain. The validator checks the for the provision of mandatory information, and will return a
 * ValidationResult dependent on the presence or absence of data
 * </p>
 */
@Component
public class MeasurementProteomicsValidator implements
    MeasurementValidator<ProteomicsMeasurementMetadata> {

  private static final Logger log = logger(MeasurementProteomicsValidator.class);

  protected final SampleInformationService sampleInformationService;

  protected final MeasurementService measurementService;

  protected final OntologyLookupService ontologyLookupService;

  protected final ProjectInformationService projectInformationService;

  @Autowired

  public MeasurementProteomicsValidator(SampleInformationService sampleInformationService,
      OntologyLookupService ontologyLookupService, MeasurementService measurementService,
      ProjectInformationService projectInformationService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.ontologyLookupService = Objects.requireNonNull(ontologyLookupService);
    this.measurementService = Objects.requireNonNull(measurementService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
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
        log.debug("Missing property header: " + pxpProperty.label());
        return false;
      }
    }
    return true;
  }

  public static Collection<String> properties() {
    return Arrays.stream(PROTEOMICS_PROPERTY.values()).map(PROTEOMICS_PROPERTY::label).toList();
  }

  @Override
  public ValidationResult validate(ProteomicsMeasurementMetadata measurementMetadata,
      ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();
    //We want to fail early so we check first if all the mandatory fields were filled
    ValidationResult mandatoryValidationResult = validationPolicy.validateMandatoryDataProvided(
        measurementMetadata);
    if (mandatoryValidationResult.containsFailures()) {
      return mandatoryValidationResult;
    }
    //If all fields were filled then we can validate the entries individually
    return validationPolicy.validateSampleIds(measurementMetadata.sampleCodes())
        .combine(validationPolicy.validateMandatoryDataProvided(measurementMetadata))
        .combine(validationPolicy.validateOrganisation(measurementMetadata.organisationId())
            .combine(validationPolicy.validateInstrument(measurementMetadata.instrumentCURI())));
  }

  /**
   * Ignores sample ids but validates measurement ids.
   *
   * @param metadata
   * @return
   * @since
   */
  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public ValidationResult validateUpdate(ProteomicsMeasurementMetadata metadata,
      ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();
    return metadata.associatedSamples().stream()
        .map(sampleCode -> validationPolicy.validationProjectRelation(sampleCode, projectId))
        .reduce(ValidationResult.successful(0),
            ValidationResult::combine).combine(validationPolicy.validateMeasurementId(
                metadata.measurementIdentifier().orElse(""))
            .combine(validationPolicy.validateMandatoryDataForUpdate(metadata))
            .combine(validationPolicy.validateOrganisation(metadata.organisationId())
                .combine(validationPolicy.validateInstrument(metadata.instrumentCURI())
                    .combine(validationPolicy.validateDigestionMethod(metadata.digestionMethod())))));
  }

  public enum PROTEOMICS_PROPERTY {
    QBIC_SAMPLE_ID("qbic sample id"),
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
    LABELING_TYPE("labeling type"),
    LABEL("label"),
    COMMENT("comment");

    private final String label;


    PROTEOMICS_PROPERTY(String propertyLabel) {
      this.label = propertyLabel;
    }

    public String label() {
      return this.label;
    }

  }

  private class ValidationPolicy {

    private static final String UNKNOWN_SAMPLE_MESSAGE = "Unknown sample with sample id \"%s\"";

    private static final String UNKNOWN_ORGANISATION_ID_MESSAGE = "The organisation ID does not seem to be a ROR ID: \"%s\"";

    private static final String UNKNOWN_INSTRUMENT_ID = "Unknown instrument id: \"%s\"";

    private static final String UNKNOWN_DIGESTION_METHOD = "Unknown digestion method: \"%s\"";

    // The unique ROR id part of the URL is described in the official documentation:
    // https://ror.readme.io/docs/ror-identifier-pattern
    private static final String ROR_ID_REGEX = "^https://ror.org/0[a-z|0-9]{6}[0-9]{2}$";

    ValidationResult validateSampleIds(Collection<SampleCode> sampleCodes) {
      if (sampleCodes.isEmpty()) {
        return ValidationResult.withFailures(1,
            List.of("A measurement must contain at least one sample reference. Provided: none"));
      }
      ValidationResult validationResult = ValidationResult.successful(
          0);
      for (SampleCode sample : sampleCodes) {
        validationResult = validationResult.combine(validateSampleId(sample));
      }
      return validationResult;
    }

    @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
    ValidationResult validationProjectRelation(SampleCode sampleCode, ProjectId projectId) {
      var projectQuery = projectInformationService.find(projectId);
      if (projectQuery.isEmpty()) {
        log.error("No project information found for projectId: " + projectId);
        throw new ValidationException("This should not happen, please try again.");
      }
      var experimentIds = projectQuery.get().experiments();
      var sampleQuery = sampleInformationService.findSampleId(sampleCode).flatMap(
          sampleIdCodeEntry -> sampleInformationService.findSample(sampleIdCodeEntry.sampleId()));
      if (sampleQuery.isEmpty()) {
        log.error("No sample information found for sample id: " + sampleCode);
        return ValidationResult.withFailures(1,
            List.of("No sample information found for sample id: %s".formatted(sampleCode.code())));
      }
      if (experimentIds.contains(sampleQuery.get().experimentId())) {
        return ValidationResult.successful(1);
      }
      return ValidationResult.withFailures(1,
          List.of("Sample ID does not belong to this project: %s".formatted(sampleCode.code())));
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

    ValidationResult validateMeasurementId(String measurementId) {
      var queryMeasurement = measurementService.findProteomicsMeasurement(measurementId);
      return queryMeasurement.map(measurement -> ValidationResult.successful(1)).orElse(
          ValidationResult.withFailures(1,
              List.of("Measurement ID: Unknown measurement for id '%s'".formatted(measurementId))));
    }

    ValidationResult validateInstrument(String instrument) {
      var result = ontologyLookupService.findByCURI(instrument);
      if (result.isPresent()) {
        return ValidationResult.successful(1);
      }
      return ValidationResult.withFailures(1,
          List.of(UNKNOWN_INSTRUMENT_ID.formatted(instrument)));
    }

    ValidationResult validateDigestionMethod(String digestionMethod) {
      if(DigestionMethod.isDigestionMethod(digestionMethod)) {
        return ValidationResult.successful(1);
      }
      return ValidationResult.withFailures(1,
          List.of(UNKNOWN_DIGESTION_METHOD.formatted(digestionMethod)));
    }

    ValidationResult validateMandatoryDataForUpdate(ProteomicsMeasurementMetadata metadata) {
      var validation = ValidationResult.successful(1);
      if (metadata.measurementIdentifier().isEmpty()) {
        validation.combine(ValidationResult.withFailures(1,
            List.of("Measurement id: missing measurement id for update")));
      } else {
        validation.combine(ValidationResult.successful(1));
      }
      if (metadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1, List.of("Organisation: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.instrumentCURI().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1, List.of("Instrument: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1, List.of("Facility: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.digestionEnzyme().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(1,
            List.of("Digestion Enzyme: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.digestionMethod().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(1,
            List.of("Digestion Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.injectionVolume().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(1,
            List.of("Injection Volume: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.lcColumn().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1, List.of("LC Column: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.lcmsMethod().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1, List.of("LCMS Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      return validation;
    }

    ValidationResult validateMandatoryDataProvided(
        ProteomicsMeasurementMetadata metadata) {
      var validation = ValidationResult.successful(0);
      if (metadata.sampleCodes().isEmpty()) {
        validation = validation.combine(
            ValidationResult.withFailures(1,
                List.of("Sample id: missing sample id reference")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1,
                List.of("Organisation: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.instrumentCURI().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1,
                List.of("Instrument: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1,
                List.of("Facility: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.digestionEnzyme().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(1,
            List.of("Digestion Enzyme: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.digestionMethod().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(1,
            List.of("Digestion Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.injectionVolume().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(1,
            List.of("Injection Volume: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.lcColumn().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1,
                List.of("LC Column: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      if (metadata.lcmsMethod().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(1,
                List.of("LCMS Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful(1));
      }
      return validation;
    }

  }

  /**
   * Describes Digestion Methods for Samples to be Measured by Proteomics
   */
  public enum DigestionMethod {
    IN_GEL("in gel"),
    IN_SOLUTION("in solution"),
    IST_PROTEOMICS_KIT("iST proteomics kit"),
    ON_BEADS("on beads");

    private final String name;

    DigestionMethod(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static boolean isDigestionMethod(String input) {
      return Arrays.stream(DigestionMethod.values()).anyMatch(o ->
              o.getName().toLowerCase().equals(input.toLowerCase()));
    }
  }

}
