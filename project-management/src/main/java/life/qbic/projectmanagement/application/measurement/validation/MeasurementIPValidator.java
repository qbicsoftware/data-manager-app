package life.qbic.projectmanagement.application.measurement.validation;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationIP;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Measurement Immunopeptidomics Validator</b>
 *
 * <p>Validator employed to check the provided user input for a measurement in the
 * immunopeptidomics domain. The validator checks for the provision of mandatory information, and
 * will return a ValidationResult dependent on the presence or absence of data.</p>
 */
@Component
public class MeasurementIPValidator {

  private static final Logger log = logger(MeasurementIPValidator.class);
  public static final String MISSING_SAMPLE_ID_REFERENCE = "Sample id: missing sample id reference";
  protected final MeasurementService measurementService;
  protected final TerminologyService terminologyService;
  protected final ProjectInformationService projectInformationService;
  private final SampleInformationService sampleInformationService;

  @Autowired
  public MeasurementIPValidator(SampleInformationService sampleInformationService,
      TerminologyService terminologyService,
      ProjectInformationService projectInformationService,
      MeasurementService measurementService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.terminologyService = Objects.requireNonNull(terminologyService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.measurementService = Objects.requireNonNull(measurementService);
  }

  /**
   * Given a collection of properties, the validator determines if they match the expected
   * properties for a QBiC-defined immunopeptidomics measurement metadata object.
   *
   * @param properties List of string representing property values in the column headers of the IP
   *                   Excel sheet
   * @return boolean indicating if all expected properties of the ip sheet could be found
   * @since 1.11.0
   */
  public static boolean isIP(Collection<String> properties) {
    if (properties.isEmpty()) {
      return false;
    }
    if (properties.size() < IP_PROPERTY.values().length) {
      log.debug("Wrong length of property header: " + properties.size());
      log.debug("Expected: " + IP_PROPERTY.values().length);
      log.debug("Provided: " + String.join(" - ", properties));
      return false;
    }
    var providedIPProperties = properties.stream().map(String::toLowerCase).toList();
    var expectedIPProperties = Arrays.stream(IP_PROPERTY.values()).map(IP_PROPERTY::label)
        .toList();
    return new HashSet<>(providedIPProperties).containsAll(expectedIPProperties);
  }

  public ValidationResult validateRegistration(MeasurementRegistrationInformationIP registration,
      String experimentId, ProjectId projectId) {
    var result = ValidationResult.successful();
    result = result.combine(validateSamples(registration, experimentId, projectId));
    result = result.combine(validateOrganisation(registration));
    result = result.combine(validateInstrument(registration));
    result = result.combine(validateMandatoryFields(registration));
    return result;
  }

  /**
   * Validates an immunopeptidomics measurement update request.
   *
   * @param update     the measurement update information
   * @param experimentId the experiment ID
   * @param projectId   the project ID
   * @return the validation result
   * @since 1.11.0
   */
  public ValidationResult validateUpdate(MeasurementUpdateInformationIP update,
      String experimentId, ProjectId projectId) {
    var validationPolicy = new ValidationPolicy();
    var result = ValidationResult.successful();
    for (String sampleId : update.measuredSamples()) {
      result = result.combine(validationPolicy.validationProjectRelation(sampleId, projectId))
          .combine(validationPolicy.validationExperimentRelation(sampleId, experimentId, projectId));
    }
    return result
        .combine(validationPolicy.validateMeasurementCode(update.measurementId()))
        .combine(validationPolicy.validateMandatoryMetadataDataForUpdate(update))
        .combine(validationPolicy.validateOrganisation(update.organisationId()))
        .combine(validationPolicy.validateInstrument(update.instrumentCURIE()));
  }

  private class ValidationPolicy {

    private static final String ROR_ID_REGEX = "^(https?://)?ror\\.org/[0-9a-zA-Z]{9}$";

    ValidationResult validateMeasurementCode(String measurementCode) {
      if (measurementCode == null || measurementCode.isBlank()) {
        return ValidationResult.successful(); // Skip, handled by validateMandatoryMetadataDataForUpdate
      }
      var queryMeasurement = measurementService.findIPMeasurement(measurementCode);
      return queryMeasurement.map(measurement -> ValidationResult.successful()).orElse(
          ValidationResult.withFailures(
              List.of(
                  "Measurement ID: Unknown measurement for id '%s'".formatted(measurementCode))));
    }

    ValidationResult validationProjectRelation(String sampleId, ProjectId projectId) {
      if (sampleId.isBlank()) {
        return ValidationResult.withFailures(
            List.of("Missing Sample id: No sample identifier was provided."));
      }
      SampleCode sampleCode = SampleCode.create(sampleId);
      var projectQuery = projectInformationService.find(projectId);
      if (projectQuery.isEmpty()) {
        log.error("No project information found for projectId: " + projectId);
        throw new RuntimeException("This should not happen, please try again.");
      }
      var experimentIds = projectQuery.get().experiments();
      var sampleQuery = sampleInformationService.findSampleId(sampleCode).flatMap(
          sampleIdCodeEntry -> sampleInformationService.findSample(sampleIdCodeEntry.sampleId()));
      if (sampleQuery.isEmpty()) {
        return ValidationResult.withFailures(
            List.of("No sample information found for sample id: %s".formatted(sampleCode.code())));
      }
      if (experimentIds.contains(sampleQuery.get().experimentId())) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of("Sample id does not belong to this project: %s".formatted(sampleCode.code())));
    }

    ValidationResult validationExperimentRelation(String sampleId, String experimentId,
        ProjectId projectId) {
      if (sampleId.isBlank()) {
        return ValidationResult.withFailures(
            List.of("Missing Sample id: No sample identifier was provided."));
      }
      SampleCode sampleCode = SampleCode.create(sampleId);
      boolean sampleContainedInExperiment = sampleInformationService.retrieveSamplesForExperiment(
          projectId,
          experimentId).stream().anyMatch(it -> it.sampleCode().equals(sampleCode));
      if (sampleContainedInExperiment) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of("Sample id does not belong to this experiment: %s".formatted(sampleCode.code())));
    }

    ValidationResult validateOrganisation(String organisationId) {
      if (organisationId == null || organisationId.isBlank()) {
        return ValidationResult.successful(); // Skip, handled by validateMandatoryMetadataDataForUpdate
      }
      if (Pattern.compile(ROR_ID_REGEX).matcher(organisationId).find()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of("The organisation ID does not seem to be a ROR ID: \"%s\"".formatted(organisationId)));
    }

    ValidationResult validateInstrument(String instrument) {
      if (instrument == null || instrument.isBlank()) {
        return ValidationResult.successful(); // Skip, handled by validateMandatoryMetadataDataForUpdate
      }
      var result = terminologyService.findByCurie(instrument);
      if (result.isPresent()) {
        return ValidationResult.successful();
      }
      return ValidationResult.withFailures(
          List.of("Unknown instrument: " + instrument));
    }

    ValidationResult validateMandatoryMetadataDataForUpdate(MeasurementUpdateInformationIP metadata) {
      var validation = ValidationResult.successful();
      if (metadata.measurementId() == null || metadata.measurementId().isEmpty()) {
        validation = validation.combine(ValidationResult.withFailures(
            List.of("Measurement ID: missing measurement ID for update")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.organisationId() == null || metadata.organisationId().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Organisation URL missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.instrumentCURIE() == null || metadata.instrumentCURIE().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Instrument missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      if (metadata.facility() == null || metadata.facility().isBlank()) {
        validation = validation.combine(
            ValidationResult.withFailures(List.of("Facility missing mandatory metadata")));
      } else {
        validation = validation.combine(ValidationResult.successful());
      }
      for (var entry : metadata.specificMetadata().entrySet()) {
        var specificMetadata = entry.getValue();
        if (specificMetadata.sampleMass() == null || specificMetadata.sampleMass().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Sample Mass missing mandatory metadata")));
        } else {
          try {
            var value = Double.parseDouble(specificMetadata.sampleMass());
            if (value < 0) {
              validation = validation.combine(
                  ValidationResult.withFailures(List.of("Sample Mass must not be negative")));
            }
          } catch (NumberFormatException e) {
            validation = validation.combine(
                ValidationResult.withFailures(List.of("Sample Mass must be a valid number")));
          }
        }
        if (specificMetadata.sampleVolume() == null || specificMetadata.sampleVolume().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Sample Volume missing mandatory metadata")));
        } else {
          try {
            var value = Double.parseDouble(specificMetadata.sampleVolume());
            if (value < 0) {
              validation = validation.combine(
                  ValidationResult.withFailures(List.of("Sample Volume must not be negative")));
            }
          } catch (NumberFormatException e) {
            validation = validation.combine(
                ValidationResult.withFailures(List.of("Sample Volume must be a valid number")));
          }
        }
        if (specificMetadata.mhcAntibody() == null || specificMetadata.mhcAntibody().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("MHC Antibody missing mandatory metadata")));
        }
        if (specificMetadata.enrichmentMethod() == null || specificMetadata.enrichmentMethod().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Enrichment method missing mandatory metadata")));
        }
        if (specificMetadata.lcmsMethod() == null || specificMetadata.lcmsMethod().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("LCMS Method missing mandatory metadata")));
        }
        if (specificMetadata.lcColumn() == null || specificMetadata.lcColumn().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("LC Column missing mandatory metadata")));
        }
        if (specificMetadata.dataAcquisition() == null || specificMetadata.dataAcquisition().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Data Acquisition missing mandatory metadata")));
        }
        if (specificMetadata.massRange() == null || specificMetadata.massRange().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Mass range missing mandatory metadata")));
        } else if (!Pattern.compile(RANGE_REGEX).matcher(specificMetadata.massRange()).matches()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Mass range must be a valid range (e.g. 1-2)")));
        }
        if (specificMetadata.retentionTimeRange() == null || specificMetadata.retentionTimeRange().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Retention time range missing mandatory metadata")));
        } else {
          try {
            double d = Double.parseDouble(specificMetadata.retentionTimeRange());
            if (d < 0) {
              validation = validation.combine(
                  ValidationResult.withFailures(List.of("Retention time range must not be negative")));
            } else if (d != (int) d) {
              validation = validation.combine(
                  ValidationResult.withFailures(List.of("Retention time range must be a valid integer")));
            }
          } catch (NumberFormatException e) {
            validation = validation.combine(
                ValidationResult.withFailures(List.of("Retention time range must be a valid integer")));
          }
        }
        if (specificMetadata.chargeRange() == null || specificMetadata.chargeRange().isBlank()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Charge range missing mandatory metadata")));
        } else if (!Pattern.compile(RANGE_REGEX).matcher(specificMetadata.chargeRange()).matches()) {
          validation = validation.combine(
              ValidationResult.withFailures(List.of("Charge range must be a valid range (e.g. 1-2)")));
        }
        // Date validation
        if (specificMetadata.prepDate() != null && !specificMetadata.prepDate().isBlank()) {
          try {
            LocalDate.parse(specificMetadata.prepDate(), DATE_FORMATTER);
          } catch (DateTimeParseException e) {
            validation = validation.combine(
                ValidationResult.withFailures(List.of("Prep Date must be formatted as YYYY-MM-DD")));
          }
        }
        if (specificMetadata.msRunDate() != null && !specificMetadata.msRunDate().isBlank()) {
          try {
            LocalDate.parse(specificMetadata.msRunDate(), DATE_FORMATTER);
          } catch (DateTimeParseException e) {
            validation = validation.combine(
                ValidationResult.withFailures(List.of("MS Run Date must be formatted as YYYY-MM-DD")));
          }
        }
      }
      return validation;
    }
  }

  private static final String RANGE_REGEX = "^\\d+(\\.\\d+)?\\s*\\-\\s*\\d+(\\.\\d+)?$";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  private ValidationResult validateMandatoryFields(MeasurementRegistrationInformationIP registration) {
    var failures = new ArrayList<String>();
    if (registration.facility() == null || registration.facility().isBlank()) {
      failures.add("Facility missing mandatory metadata");
    }
    for (var entry : registration.specificMetadata().entrySet()) {
      var metadata = entry.getValue();
      if (metadata.sampleMass() == null || metadata.sampleMass().isBlank()) {
        failures.add("Sample Mass missing mandatory metadata");
      } else {
        try {
          var value = Double.parseDouble(metadata.sampleMass());
          if (value < 0) {
            failures.add("Sample Mass must not be negative");
          }
        } catch (NumberFormatException e) {
          failures.add("Sample Mass must be a valid number");
        }
      }
      if (metadata.sampleVolume() == null || metadata.sampleVolume().isBlank()) {
        failures.add("Sample Volume missing mandatory metadata");
      } else {
        try {
          var value = Double.parseDouble(metadata.sampleVolume());
          if (value < 0) {
            failures.add("Sample Volume must not be negative");
          }
        } catch (NumberFormatException e) {
          failures.add("Sample Volume must be a valid number");
        }
      }
      if (metadata.mhcAntibody() == null || metadata.mhcAntibody().isBlank()) {
        failures.add("MHC Antibody missing mandatory metadata");
      }
      if (metadata.enrichmentMethod() == null || metadata.enrichmentMethod().isBlank()) {
        failures.add("Enrichment method missing mandatory metadata");
      }
      if (metadata.lcmsMethod() == null || metadata.lcmsMethod().isBlank()) {
        failures.add("LCMS Method missing mandatory metadata");
      }
      if (metadata.lcColumn() == null || metadata.lcColumn().isBlank()) {
        failures.add("LC Column missing mandatory metadata");
      }
      if (metadata.dataAcquisition() == null || metadata.dataAcquisition().isBlank()) {
        failures.add("Data Acquisition missing mandatory metadata");
      }
      if (metadata.massRange() == null || metadata.massRange().isBlank()) {
        failures.add("Mass range missing mandatory metadata");
      } else if (!Pattern.compile(RANGE_REGEX).matcher(metadata.massRange()).matches()) {
        failures.add("Mass range must be a valid range (e.g. 1-2)");
      }
      if (metadata.retentionTimeRange() == null || metadata.retentionTimeRange().isBlank()) {
        failures.add("Retention time range missing mandatory metadata");
      } else {
        try {
          double d = Double.parseDouble(metadata.retentionTimeRange());
          if (d < 0) {
            failures.add("Retention time range must not be negative");
          } else if (d != (int) d) {
            failures.add("Retention time range must be a valid integer");
          }
        } catch (NumberFormatException e) {
          failures.add("Retention time range must be a valid integer");
        }
      }
      if (metadata.chargeRange() == null || metadata.chargeRange().isBlank()) {
        failures.add("Charge range missing mandatory metadata");
      } else if (!Pattern.compile(RANGE_REGEX).matcher(metadata.chargeRange()).matches()) {
        failures.add("Charge range must be a valid range (e.g. 1-2)");
      }
      if (metadata.ionMobilityRange() != null && !metadata.ionMobilityRange().isBlank()
          && !Pattern.compile(RANGE_REGEX).matcher(metadata.ionMobilityRange()).matches()) {
        failures.add("Ion Mobility Range must be a valid range (e.g. 1-2)");
      }
      if (metadata.prepDate() != null && !metadata.prepDate().isBlank()) {
        try {
          LocalDate.parse(metadata.prepDate(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
          failures.add("Prep Date must be formatted as YYYY-MM-DD");
        }
      }
      if (metadata.msRunDate() != null && !metadata.msRunDate().isBlank()) {
        try {
          LocalDate.parse(metadata.msRunDate(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
          failures.add("MS Run Date must be formatted as YYYY-MM-DD");
        }
      }
    }
    if (failures.isEmpty()) {
      return ValidationResult.successful();
    }
    return ValidationResult.withFailures(failures);
  }

  private ValidationResult validateInstrument(MeasurementRegistrationInformationIP registration) {
    if (registration.instrumentCURIE() == null || registration.instrumentCURIE().isBlank()) {
      return ValidationResult.withFailures(List.of("Instrument missing mandatory metadata"));
    }
    var instrument = terminologyService.findByCurie(registration.instrumentCURIE());
    if (instrument.isEmpty()) {
      return ValidationResult.withFailures(
          List.of("Unknown instrument: " + registration.instrumentCURIE()));
    }
    return ValidationResult.successful();
  }

  private static final String ROR_ID_REGEX = "^(https?://)?ror\\.org/[0-9a-zA-Z]{9}$";

  private ValidationResult validateOrganisation(MeasurementRegistrationInformationIP registration) {
    if (registration.organisationId() == null || registration.organisationId().isBlank()) {
      return ValidationResult.withFailures(List.of("Organisation URL missing mandatory metadata"));
    }
    if (!Pattern.compile(ROR_ID_REGEX).matcher(registration.organisationId()).find()) {
      return ValidationResult.withFailures(
          List.of("The organisation ID does not seem to be a ROR ID: \""
              + registration.organisationId() + "\""));
    }
    return ValidationResult.successful();
  }

  private ValidationResult validateSamples(MeasurementRegistrationInformationIP registration,
      String experimentId, ProjectId projectId) {
    if (registration.measuredSamples().isEmpty()) {
      return ValidationResult.withFailures(List.of(MISSING_SAMPLE_ID_REFERENCE));
    }
    var failures = new ArrayList<String>();
    for (String sampleId : registration.measuredSamples()) {
      try {
        var sampleCode = SampleCode.create(sampleId);
        var sampleQuery = sampleInformationService.findSampleId(sampleCode);
        if (sampleQuery.isEmpty()) {
          failures.add("Unknown sample: " + sampleId);
          continue;
        }
        var sample = sampleQuery.get();
        var samples = sampleInformationService.retrieveSamplesByIds(List.of(sample.sampleId()));
        if (samples.isEmpty()) {
          failures.add("Sample not found: " + sampleId);
          continue;
        }
        var associatedExperimentsFromProject = projectInformationService.find(projectId)
            .orElseThrow().experiments();
        if (!associatedExperimentsFromProject.contains(samples.getFirst().experimentId())) {
          failures.add("Sample " + sampleId + " is not part of project " + projectId);
        }
      } catch (IllegalArgumentException e) {
        failures.add("Invalid sample id: " + sampleId);
      }
    }
    if (failures.isEmpty()) {
      return ValidationResult.successful();
    }
    return ValidationResult.withFailures(failures);
  }

  public enum IP_PROPERTY {
    QBIC_SAMPLE_ID("qbic sample id"),
    SAMPLE_NAME("sample name"),
    MEASUREMENT_NAME("measurement name"),
    ORGANISATION_ID("organisation url"),
    FACILITY("facility"),
    SAMPLE_MASS("sample mass (mg)"),
    SAMPLE_VOLUME("sample volume (decimal)"),
    CYCLE_FRACTION_NAME("cycle/fraction name"),
    MHC_ANTIBODY("mhc antibody"),
    MHC_TYPING_METHOD("mhc typing method"),
    ENRICHMENT_METHOD("enrichment method"),
    PREP_DATE("prep date"),
    MS_RUN_DATE("ms run date"),
    INSTRUMENT("instrument"),
    LCMS_METHOD("lcms method"),
    LC_COLUMN("lc column"),
    DATA_ACQUISITION("data acquisition"),
    MASS_RANGE("mass range (m/z)"),
    RETENTION_TIME_RANGE("retention time range (min)"),
    CHARGE_RANGE("charge range"),
    ION_MOBILITY_RANGE("ion mobility range (1/k0)"),
    COMMENT("comment");

    private final String label;

    IP_PROPERTY(String propertyLabel) {
      this.label = propertyLabel;
    }

    public String label() {
      return this.label;
    }
  }
}
