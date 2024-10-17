package life.qbic.projectmanagement.application.sample;

import static java.util.Objects.isNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.ValidationResultWithPayload;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
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
public class SampleValidation {

  private final SampleInformationService sampleInformationService;

  private final ExperimentInformationService experimentInformationService;

  private final TerminologyService terminologyService;

  private final SpeciesLookupService speciesLookupService;

  @Autowired
  public SampleValidation(SampleInformationService sampleInformationService,
      ExperimentInformationService experimentInformationService,
      TerminologyService terminologyService, SpeciesLookupService speciesLookupService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.terminologyService = Objects.requireNonNull(terminologyService);
    this.speciesLookupService = Objects.requireNonNull(speciesLookupService);
  }

  /**
   * Creates a lookup table for conditions having their String representation as key.
   * <p>
   * The String representation is done with the {@link PropertyConversion#toString(Condition)}
   * method.
   *
   * @param conditions the conditions to take for the lookup table build
   * @return the lookup table
   * @since 1.5.0
   */
  private static Map<String, ExperimentalGroup> conditionLookup(
      List<ExperimentalGroup> conditions) {
    return conditions.stream()
        .collect(Collectors.toMap(group -> PropertyConversion.toString(group.condition()),
            Function.identity()));
  }

  /**
   * Validates metadata for a not yet registered sample. The validation does not look for any sample
   * id and no sample information lookups are done in this case.
   * <p>
   * If the client wants to validate the sample id as well, please refer to
   * {@link SampleValidation#validateExistingSample(String, String, String, String, String, String,
   * String, String, String, String, String)}
   *
   * @param sampleName          the name of the sample
   * @param biologicalReplicate the biological replicate
   * @param condition           the condition the sample was collected from
   * @param species             the species the sample was taken from
   * @param specimen            the specimen of the sample
   * @param analyte             the analyte that was extracted from the specimen
   * @param analysisMethod      the method applied on the analyte
   * @param comment             the comment associated with the sample
   * @param experimentId        the experiment id of the experiment the sample belongs to
   * @param projectId           the project id of project the experiment belongs to
   * @return the report of the validation
   * @since 1.5.0
   */
  public ValidationResultWithPayload<SampleMetadata> validateNewSample(String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      String experimentId,
      String projectId) {

    var experimentQuery = experimentInformationService.find(projectId,
        ExperimentId.parse(experimentId));
    if (experimentQuery.isEmpty()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Unknown experiment")), null);
    }
    var experiment = experimentQuery.orElseThrow();
    var experimentalGroupLookupTable = conditionLookup(experiment.getExperimentalGroups());

    return validateForNewSample(sampleName,
        biologicalReplicate,
        condition,
        species,
        specimen,
        analyte,
        analysisMethod,
        comment,
        experimentId,
        experimentalGroupLookupTable);
  }

  private ValidationResultWithPayload<SampleMetadata> validateForNewSample(
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      String experimentId,
      Map<String, ExperimentalGroup> experimentalGroupLookupTable) {

    var sampleNameValidation = validateSampleName(sampleName);
    var experimentalGroupValidation = validateExperimentalGroupForCondition(condition,
        experimentalGroupLookupTable);
    var analysisMethodValidation = validateAnalysisMethod(analysisMethod);
    var speciesValidation = validateSpecies(species);
    var specimenValidation = validateSpecimen(specimen);
    var analyteValidation = validateAnalyte(analyte);

    ValidationResult combinedValidationResult = ValidationResult.successful()
        .combine(sampleNameValidation.validationResult())
        .combine(experimentalGroupValidation.validationResult())
        .combine(analysisMethodValidation.validationResult())
        .combine(speciesValidation.validationResult())
        .combine(specimenValidation.validationResult())
        .combine(analyteValidation.validationResult());
    var metadata = combinedValidationResult.containsFailures()
        ? null
        : SampleMetadata.createNew(
            sampleNameValidation.payload(),
            analysisMethodValidation.payload(),
            biologicalReplicate,
            experimentalGroupValidation.payload(),
            speciesValidation.payload(),
            specimenValidation.payload(),
            analyteValidation.payload(),
            comment,
            experimentId);
    return new ValidationResultWithPayload<>(combinedValidationResult, metadata);
  }

  private ValidationResultWithPayload<String> validateSampleName(String sampleName) {
    if (isNull(sampleName) || sampleName.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing sample name")), null);
    }
    return new ValidationResultWithPayload<>(ValidationResult.successful(), sampleName);
  }

  private ValidationResultWithPayload<Long> validateExperimentalGroupForCondition(String condition,
      Map<String, ExperimentalGroup> conditionsLookupTable) {
    if (isNull(condition) || condition.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing condition")), null);
    }
    if (conditionsLookupTable.containsKey(condition)) {
      return new ValidationResultWithPayload<>(ValidationResult.successful(),
          conditionsLookupTable.get(condition).id());
    } else {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Unknown condition: '" + condition + "'")), null);
    }
  }

  private ValidationResultWithPayload<AnalysisMethod> validateAnalysisMethod(
      String analysisMethod) {
    if (analysisMethod == null || analysisMethod.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("No analysis method provided")), null);
    }
    return AnalysisMethod.forAbbreviation(analysisMethod)
        .map(it -> new ValidationResultWithPayload<>(ValidationResult.successful(), it))
        .orElse(new ValidationResultWithPayload<>(
            ValidationResult.withFailures(List.of("Unknown analysis: '" + analysisMethod + "'")),
            null));
  }

  private ValidationResultWithPayload<OntologyTerm> validateSpecies(String species) {
    if (isNull(species) || species.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing species")),
          null);
    }
    var extractedTerm = PropertyConversion.extractCURIE(species);
    if (extractedTerm.isEmpty()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing CURIE in species: '" + species + "'")),
          null);
    }
    var speciesLookup = speciesLookupService.findByCURI(extractedTerm.get());
    return speciesLookup
        .map(OntologyTerm::from)
        .map(it ->
            new ValidationResultWithPayload<>(ValidationResult.successful(), it))
        .orElse(new ValidationResultWithPayload<>(
            ValidationResult.withFailures(List.of("Unknown species: '" + species + "'")), null));
  }

  private ValidationResultWithPayload<OntologyTerm> validateSpecimen(String specimen) {
    if (isNull(specimen) || specimen.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing specimen")),
          null);
    }
    var extractedTerm = PropertyConversion.extractCURIE(specimen);
    if (extractedTerm.isEmpty()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing CURIE in specimen: '" + specimen + "'")),
          null);
    }
    var speciesLookup = terminologyService.findByCurie(extractedTerm.get());
    return speciesLookup
        .map(it ->
            new ValidationResultWithPayload<>(ValidationResult.successful(), it))
        .orElse(new ValidationResultWithPayload<>(
            ValidationResult.withFailures(List.of("Unknown specimen: '" + specimen + "'")), null));
  }

  private ValidationResultWithPayload<OntologyTerm> validateAnalyte(String analyte) {
    if (isNull(analyte) || analyte.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing analyte")),
          null);
    }
    var extractedTerm = PropertyConversion.extractCURIE(analyte);
    if (extractedTerm.isEmpty()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing CURIE in analyte: '" + analyte + "'")),
          null);
    }
    var speciesLookup = terminologyService.findByCurie(extractedTerm.get());
    return speciesLookup
        .map(it ->
            new ValidationResultWithPayload<>(ValidationResult.successful(), it))
        .orElse(new ValidationResultWithPayload<>(
            ValidationResult.withFailures(List.of("Unknown analyte: '" + analyte + "'")), null));
  }

  private ValidationResultWithPayload<SampleId> validateSampleIdForSampleCode(String sampleCode) {
    if (sampleCode.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing sample id")),
          null
      );
    }
    var sampleIdQuery = sampleInformationService.findSampleId(SampleCode.create(sampleCode));
    if (sampleIdQuery.isEmpty()) {
      return new ValidationResultWithPayload<>(ValidationResult.withFailures(List.of(
          "Unknown sample id: '" + sampleCode + "'")), null);
    }
    var sampleId = sampleIdQuery.orElseThrow().sampleId();
    return new ValidationResultWithPayload<>(ValidationResult.successful(), sampleId);
  }

  /**
   * Validates the metadata for a sample that has previously been registered. A registered sample
   * has a sample code (sample id to the user) and an internal technical sample id.
   * <p>
   * The method verifies the existence of a sample with the provided sample code and resolves it to
   * the matching internal sample id.
   * <p>
   * All other validation steps are equal to a
   * {@link SampleValidation#validateNewSample(String, String, String, String, String, String,
   * String, String, String, String)} call.
   *
   * @param sampleCode     the sample code of the sample, known as sample id to the user
   * @param condition      the condition the sample was collected from
   * @param species        the species the sample was taken from
   * @param specimen       the specimen of the sample
   * @param analyte        the analyte that was extracted from the specimen
   * @param analysisMethod the method applied on the analyte
   * @param experimentId   the experiment the sample belongs to
   * @param projectId      the project the sample belongs to
   * @return a {@link ValidationResult} with detailed information about the validation
   * @since 1.5.0
   */
  public ValidationResultWithPayload<SampleMetadata> validateExistingSample(String sampleCode,
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      String experimentId,
      String projectId) {

    var experimentQuery = experimentInformationService.find(projectId,
        ExperimentId.parse(experimentId));
    if (experimentQuery.isEmpty()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Unknown experiment")), null);
    }

    var experiment = experimentQuery.orElseThrow();
    var experimentalGroupLookupTable = conditionLookup(experiment.getExperimentalGroups());

    var sampleIdValidation = validateSampleIdForSampleCode(sampleCode);
    var sampleNameValidation = validateSampleName(sampleName);
    var experimentalGroupValidation = validateExperimentalGroupForCondition(condition,
        experimentalGroupLookupTable);
    var analysisMethodValidation = validateAnalysisMethod(analysisMethod);
    var speciesValidation = validateSpecies(species);
    var specimenValidation = validateSpecimen(specimen);
    var analyteValidation = validateAnalyte(analyte);

    ValidationResult combinedValidationResult = ValidationResult.successful()
        .combine(sampleIdValidation.validationResult())
        .combine(sampleNameValidation.validationResult())
        .combine(experimentalGroupValidation.validationResult())
        .combine(analysisMethodValidation.validationResult())
        .combine(speciesValidation.validationResult())
        .combine(specimenValidation.validationResult())
        .combine(analyteValidation.validationResult());
    var metadata = combinedValidationResult.containsFailures()
        ? null
        : SampleMetadata.createUpdate(
            sampleIdValidation.payload(),
            sampleCode,
            sampleNameValidation.payload(),
            analysisMethodValidation.payload(),
            biologicalReplicate,
            experimentalGroupValidation.payload(),
            speciesValidation.payload(),
            specimenValidation.payload(),
            analyteValidation.payload(),
            comment,
            experimentId);
    return new ValidationResultWithPayload<>(combinedValidationResult, metadata);
  }

}
