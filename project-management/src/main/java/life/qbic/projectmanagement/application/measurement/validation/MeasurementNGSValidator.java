package life.qbic.projectmanagement.application.measurement.validation;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ValidationException;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * <b>Measurement NGS Validator</b>
 *
 * <p>Validator employed to check the provided user input for a measurement in the ngs domain.
 * The validator checks the for the provision of mandatory information, and will return a
 * ValidationResult dependent on the presence or absence of data
 * </p>
 */
@Component
public class MeasurementNGSValidator implements
    MeasurementValidator<NGSMeasurementMetadata> {

  private static final Logger log = logger(MeasurementNGSValidator.class);
  protected final MeasurementService measurementService;
  protected final TerminologyService terminologyService;
  protected final ProjectInformationService projectInformationService;
  private final SampleInformationService sampleInformationService;

  @Autowired
  public MeasurementNGSValidator(SampleInformationService sampleInformationService,
      TerminologyService terminologyService,
      ProjectInformationService projectInformationService,
      MeasurementService measurementService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.terminologyService = Objects.requireNonNull(terminologyService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.measurementService = Objects.requireNonNull(measurementService);
  }

  /**
   * Given a collection of properties, the validator determines if they mach the expected properties
   * for a QBiC-defined NGS measurement metadata object.
   *
   * @param properties List of string representing property values in the column headers of the NGS
   *                   Excel sheet
   * @return boolean indicating if all expected properties of the ngs sheet could be found
   * @since 1.0.0
   */
  public static boolean isNGS(Collection<String> properties) {
    if (properties.isEmpty()) {
      return false;
    }
    if (properties.size() < NGS_PROPERTY.values().length) {
      log.debug("Wrong length of property header: " + properties().size());
      log.debug("Expected: " + NGS_PROPERTY.values().length);
      log.debug("Provided: " + String.join(" - ", properties));
      return false;
    }
    var providedNGSProperties = properties.stream().map(String::toLowerCase).toList();
    var expectedNGSProperties = Arrays.stream(NGS_PROPERTY.values()).map(NGS_PROPERTY::label)
        .toList();
    return new HashSet<>(providedNGSProperties).containsAll(expectedNGSProperties);
  }

  public static Collection<String> properties() {
    return Arrays.stream(NGS_PROPERTY.values()).map(NGS_PROPERTY::label).toList();
  }

  @Override
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validate(NGSMeasurementMetadata measurementMetadata,
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

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateRegistration(MeasurementRegistrationInformationNGS registration,
      ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();

    ValidationResult mandatoryValidationResult = validationPolicy.validateMandatoryDataRegistration(
        registration);
    if (mandatoryValidationResult.containsFailures()) {
      return mandatoryValidationResult;
    }
    return validationPolicy.validateSampleIdsAsString(registration.measuredSamples())
        .combine(validationPolicy.validateMandatoryDataRegistration(registration))
        .combine(validationPolicy.validateOrganisation(registration.organisationId()))
        .combine(validationPolicy.validateInstrument(registration.instrumentCURIE()));
  }

  /**
   * Ignores sample ids but validates measurement ids.
   *
   * @param metadata, {@link NGSMeasurementMetadata} of the measurement to be updated
   * @param projectId Id of the project to which the measurement belongs to, necessary to check user
   *                  permission
   * @return ValidationResult
   */
  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public ValidationResult validateUpdate(NGSMeasurementMetadata metadata, ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();
    return validationPolicy.validationProjectRelation(metadata.associatedSample(), projectId)
        .combine(
            validationPolicy.validateMeasurementCode(metadata.measurementIdentifier().orElse(""))
                .combine(validationPolicy.validateMandatoryDataForUpdate(metadata))
                .combine(validationPolicy.validateOrganisation(metadata.organisationId())
                    .combine(validationPolicy.validateInstrument(metadata.instrumentCURI()))));
  }

  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public ValidationResult validateUpdate(MeasurementUpdateInformationNGS metadata, ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();
    var result = ValidationResult.successful();
    for (String sampleId : metadata.measuredSamples()) {
      result.combine(validationPolicy.validationProjectRelation(SampleCode.create(sampleId), projectId));
    }
    return result.combine(validationPolicy.validateMeasurementCode(metadata.measurementId()))
        .combine(validationPolicy.validateMandatoryMetadataDataForUpdate(metadata))
        .combine(validationPolicy.validateOrganisation(metadata.organisationId()))
        .combine(validationPolicy.validateInstrument(metadata.instrumentCURIE()));
  }


  public enum NGS_PROPERTY {
    QBIC_SAMPLE_ID("qbic sample id"),
    SAMPLE_LABEL("sample name"),
    ORGANISATION_ID("organisation id"),
    FACILITY("facility"),
    INSTRUMENT("instrument"),
    SEQUENCING_READ_TYPE("sequencing read type"),
    LIBRARY_KIT("library kit"),
    FLOW_CELL("flow cell"),
    SEQUENCING_RUN_PROTOCOL("sequencing run protocol"),
    SAMPLE_POOL_GROUP("sample pool group"),
    INDEX_I7("index i7"),
    INDEX_I5("index i5"),
    COMMENT("comment");

    private final String label;

    NGS_PROPERTY(String propertyLabel) {
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

    // The unique ROR id part of the URL is described in the official documentation:
    // https://ror.readme.io/docs/ror-identifier-pattern
    private static final String ROR_ID_REGEX = "^https://ror.org/0[a-z|0-9]{6}[0-9]{2}$";

    ValidationResult validateMeasurementCode(String measurementCode) {
      var queryMeasurement = measurementService.findNGSMeasurement(measurementCode);
      return queryMeasurement.map(measurement -> ValidationResult.successful()).orElse(
          ValidationResult.withFailures(
              List.of(
                  "Measurement ID: Unknown measurement for id '%s'".formatted(measurementCode))));
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
        return ValidationResult.withFailures(
            List.of("No sample information found for sample id: %s".formatted(sampleCode.code())));
      }
      if (experimentIds.contains(sampleQuery.get().experimentId())) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of("Sample ID does not belong to this project: %s".formatted(sampleCode.code())));
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

    ValidationResult validateSampleId(SampleCode sampleCode) {
      var queriedSampleEntry = sampleInformationService.findSampleId(sampleCode);
      if (queriedSampleEntry.isPresent()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of(UNKNOWN_SAMPLE_MESSAGE.formatted(sampleCode.code())));
    }

    ValidationResult validateOrganisation(String organisationId) {
      if (Pattern.compile(ROR_ID_REGEX).matcher(organisationId).find()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of(UNKNOWN_ORGANISATION_ID_MESSAGE.formatted(organisationId)));
    }

    ValidationResult validateInstrument(String instrument) {
      var result = terminologyService.findByCurie(instrument);
      if (result.isPresent()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of(UNKNOWN_INSTRUMENT_ID.formatted(instrument)));
    }

    @Deprecated
    ValidationResult validateMandatoryDataProvided(
        NGSMeasurementMetadata measurementMetadata) {
      var validation = ValidationResult.successful();
      if (measurementMetadata.sampleCodes().isEmpty()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Sample id: missing sample id reference")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (measurementMetadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Organisation: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (measurementMetadata.instrumentCURI().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Instrument: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (measurementMetadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Facility: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (measurementMetadata.sequencingReadType().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Read Type: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      return validation;
    }

    ValidationResult validateMandatoryDataRegistration(
        MeasurementRegistrationInformationNGS metadata) {
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
      if (metadata.instrumentCURIE().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Instrument: missing mandatory metadata")));
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
      if (metadata.sequencingReadType().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Read Type: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }

      var missingIndices = new MissingIndices(() -> metadata);

      validation = validation.combine(missingIndices.execute());

      return validation;
    }

    ValidationResult validateMandatoryMetadataDataForUpdate(
        MeasurementUpdateInformationNGS metadata) {
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
      if (metadata.instrumentCURIE().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Instrument: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Facility: missing mandatory meta;data")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.sequencingReadType().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Read Type: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      return validation;
    }

    @Deprecated
    ValidationResult validateMandatoryDataForUpdate(NGSMeasurementMetadata metadata) {
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
      if (metadata.instrumentCURI().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Instrument: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Facility: missing mandatory meta;data")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.sequencingReadType().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(
                List.of("Read Type: missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      return validation;
    }
  }
}

