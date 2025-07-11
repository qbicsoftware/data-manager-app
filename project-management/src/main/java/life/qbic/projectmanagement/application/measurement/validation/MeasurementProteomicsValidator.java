package life.qbic.projectmanagement.application.measurement.validation;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ValidationException;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationPxP;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
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

  protected final TerminologyService terminologyService;

  protected final ProjectInformationService projectInformationService;

  @Autowired
  public MeasurementProteomicsValidator(SampleInformationService sampleInformationService,
      TerminologyService terminologyService, MeasurementService measurementService,
      ProjectInformationService projectInformationService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.terminologyService = Objects.requireNonNull(terminologyService);
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
      log.debug("No properties found");
      return false;
    }
    if (properties.size() < PROTEOMICS_PROPERTY.values().length) {
      log.debug("Wrong length of property header: " + properties().size());
      log.debug("Expected: " + PROTEOMICS_PROPERTY.values().length);
      log.debug("Provided: " + String.join(" - ", properties));
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
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
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
    return validationPolicy.validateSampleId(measurementMetadata.sampleCode())
        .combine(validationPolicy.validateMandatoryDataProvided(measurementMetadata))
        .combine(validationPolicy.validateOrganisation(measurementMetadata.organisationId())
            .combine(validationPolicy.validateMsDevice(measurementMetadata.msDeviceCURIE())));
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateRegistration(MeasurementRegistrationInformationPxP metadata,
      ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();
    ValidationResult mandatoryValidationResult = validationPolicy.validateMandatoryDataProvided(
        metadata);
    if (mandatoryValidationResult.containsFailures()) {
      return mandatoryValidationResult;
    }

    var validation = validationPolicy.validateSampleIdsAsString(metadata.measuredSamples())
        .combine(validationPolicy.validateMandatoryDataProvided(metadata))
        .combine(validationPolicy.validateOrganisation(metadata.organisationId())
        .combine(validationPolicy.validateMsDevice(metadata.msDeviceCURIE())));

    // For the sample-specific metadata
    var missingLabelsValidation = new MissingLabel(() -> metadata).execute();
    var distinctLabelsValidation = new HasDistinctLabels(() -> metadata).execute();

    return validation.combine(missingLabelsValidation)
        .combine(distinctLabelsValidation);
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
    return validationPolicy.validateSampleId(metadata.associatedSample())
        .combine(validationPolicy.validationProjectRelation(metadata.associatedSample(), projectId))
        .combine(
            validationPolicy.validateMeasurementCode(metadata.measurementIdentifier().orElse(""))
                .combine(validationPolicy.validateMandatoryDataForUpdate(metadata))
                .combine(validationPolicy.validateOrganisation(metadata.organisationId())
                    .combine(validationPolicy.validateMsDevice(metadata.msDeviceCURIE())
                        .combine(
                            validationPolicy.validateDigestionMethod(
                                metadata.digestionMethod())))));
  }

  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','WRITE')")
  public ValidationResult validateUpdate(MeasurementUpdateInformationPxP metadata, ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();
    var result = ValidationResult.successful();
    for (String sampleId : metadata.measuredSamples()) {
      result.combine(validationPolicy.validationProjectRelation(SampleCode.create(sampleId), projectId));
    }
    return result.combine(validationPolicy.validateSampleIdsAsString(metadata.measuredSamples()))
        .combine(validationPolicy.validateMandatoryMetadataDataForUpdate(metadata))
        .combine(validationPolicy.validateOrganisation(metadata.organisationId()))
        .combine(validationPolicy.validateMsDevice(metadata.msDeviceCURIE()))
        .combine(validationPolicy.validateDigestionMethod(metadata.digestionMethod()));
  }

  public enum PROTEOMICS_PROPERTY {
    QBIC_SAMPLE_ID("qbic sample id"),
    SAMPLE_LABEL("sample name"),
    TECHNICAL_REPLICATE_NAME("technical replicate"),
    ORGANISATION_ID("organisation id"),
    FACILITY("facility"),
    MS_DEVICE("ms device"),
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

    public static boolean isDigestionMethod(String input) {
      return Arrays.stream(DigestionMethod.values()).anyMatch(o ->
          o.getName().equalsIgnoreCase(input));
    }

    public String getName() {
      return name;
    }

    public static List<String> getOptions() {
      return Arrays.stream(values()).map(DigestionMethod::getName).toList();
    }
  }

  private class ValidationPolicy {

    private static final String UNKNOWN_SAMPLE_MESSAGE = "Unknown sample with sample id \"%s\"";

    private static final String UNKNOWN_ORGANISATION_ID_MESSAGE = "The organisation ID does not seem to be a ROR ID: \"%s\"";

    private static final String UNKNOWN_MS_DEVICE_ID = "Unknown ms device id: \"%s\"";

    private static final String UNKNOWN_DIGESTION_METHOD = "Unknown digestion method: \"%s\"";

    // The unique ROR id part of the URL is described in the official documentation:
    // https://ror.readme.io/docs/ror-identifier-pattern
    private static final String ROR_ID_REGEX = "^https://ror.org/0[a-z|0-9]{6}[0-9]{2}$";

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
        return ValidationResult.withFailures(
            List.of("No sample information found for sample id: %s".formatted(sampleCode.code())));
      }
      if (experimentIds.contains(sampleQuery.get().experimentId())) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of("Sample ID does not belong to this project: %s".formatted(sampleCode.code())));
    }

    ValidationResult validateSampleId(SampleCode sampleCode) {
      var queriedSampleEntry = sampleInformationService.findSampleId(sampleCode);
      if (queriedSampleEntry.isPresent()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of(UNKNOWN_SAMPLE_MESSAGE.formatted(sampleCode.code())));
    }

    ValidationResult validateSampleIds(Collection<SampleCode> sampleCodes) {
      if (sampleCodes.isEmpty()) {
        return ValidationResult.withFailures(
            List.of("A measurement must contain at least one sample reference. Provided: none"));
      }
      ValidationResult validationResult = ValidationResult.successful(
      );
      for (SampleCode sample : sampleCodes) {
        validationResult = validationResult.combine(validateSampleId(sample));
      }
      return validationResult;
    }

    ValidationResult validateSampleIdsAsString(Collection<String> sampleIds) {
      var sampleCodes = sampleIds.stream().map(SampleCode::create).toList();
      return validateSampleIds(sampleCodes);
    }

    ValidationResult validateOrganisation(String organisationId) {
      if (Pattern.compile(ROR_ID_REGEX).matcher(organisationId).find()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of(UNKNOWN_ORGANISATION_ID_MESSAGE.formatted(organisationId)));
    }

    ValidationResult validateMeasurementCode(String measurementCode) {
      var queryMeasurement = measurementService.findProteomicsMeasurement(measurementCode);
      return queryMeasurement.map(measurement -> ValidationResult.successful()).orElse(
          ValidationResult.withFailures(
              List.of(
                  "Measurement Code: Unknown measurement for id '%s'".formatted(measurementCode))));
    }

    ValidationResult validateMsDevice(String msDevice) {
      var result = terminologyService.findByCurie(msDevice);
      if (result.isPresent()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of(UNKNOWN_MS_DEVICE_ID.formatted(msDevice)));
    }

    ValidationResult validateDigestionMethod(String digestionMethod) {
      if (DigestionMethod.isDigestionMethod(digestionMethod)) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of(UNKNOWN_DIGESTION_METHOD.formatted(digestionMethod)));
    }

    @Deprecated
    ValidationResult validateMandatoryDataForUpdate(ProteomicsMeasurementMetadata metadata) {
      var validation = ValidationResult.successful();
      if (metadata.measurementIdentifier().isEmpty()) {
        validation.combine(ValidationResult.withFailures(
            List.of("Measurement id: missing measurement id for update")));
      } else {
        validation.combine(ValidationResult.successful());
      }
      if (metadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Organisation: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.msDeviceCURIE().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("MS Device: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Facility: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionEnzyme().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Enzyme: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionMethod().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.lcColumn().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("LC Column: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      return validation;
    }

    ValidationResult validateMandatoryMetadataDataForUpdate(MeasurementUpdateInformationPxP metadata) {
      var validation = ValidationResult.successful();
      if (metadata.measurementId().isEmpty()) {
        validation.combine(ValidationResult.withFailures(
            List.of("Measurement id: missing measurement id for update")));
      } else {
        validation.combine(ValidationResult.successful());
      }
      if (metadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Organisation: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.msDeviceCURIE().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("MS Device: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Facility: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionEnzyme().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Enzyme: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionMethod().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.lcColumn().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("LC Column: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      return validation;
    }


    ValidationResult validateMandatoryDataProvided(MeasurementRegistrationInformationPxP metadata) {
      var validation = ValidationResult.successful();
      if (metadata.measuredSamples().isEmpty()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Sample id: missing sample id reference")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Organisation: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.msDeviceCURIE().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("MS Device: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Facility: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionEnzyme().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Enzyme: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionMethod().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.lcColumn().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("LC Column: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      return validation;
    }

    @Deprecated
    ValidationResult validateMandatoryDataProvided(
        ProteomicsMeasurementMetadata metadata) {
      var validation = ValidationResult.successful();
      if (metadata.sampleCode() == null) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Sample id: missing sample id reference")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Organisation: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.msDeviceCURIE().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("MS Device: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Facility: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionEnzyme().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Enzyme: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.digestionMethod().isBlank()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Digestion Method: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.lcColumn().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("LC Column: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      return validation;
    }

  }

}
