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
          var value = Integer.parseInt(metadata.retentionTimeRange());
          if (value < 0) {
            failures.add("Retention time range must not be negative");
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
