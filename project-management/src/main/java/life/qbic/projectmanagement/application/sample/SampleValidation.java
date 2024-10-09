package life.qbic.projectmanagement.application.sample;

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
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
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
public class SampleValidation {

  private final SampleInformationService sampleInformationService;

  private final ExperimentInformationService experimentInformationService;

  private final TerminologyService terminologyService;

  private final SpeciesLookupService speciesLookupService;

  private ThreadLocal<SampleMetadata> assembledMetadata;

  @Autowired
  public SampleValidation(SampleInformationService sampleInformationService,
      ExperimentInformationService experimentInformationService,
      TerminologyService terminologyService, SpeciesLookupService speciesLookupService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.terminologyService = Objects.requireNonNull(terminologyService);
    this.speciesLookupService = Objects.requireNonNull(speciesLookupService);
    this.assembledMetadata = new ThreadLocal<>();
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
   * String, String, String)}
   *
   * @param sampleName
   * @param condition      the condition the sample was collected from
   * @param species        the species the sample was taken from
   * @param specimen       the specimen of the sample
   * @param analyte        the analyte that was extracted from the specimen
   * @param analysisMethod the method applied on the analyte
   * @param experimentId   the experiment id of the experiment the sample belongs to
   * @param projectId      the project id of project the experiment belongs to
   * @return the report of the validation
   * @since 1.5.0
   */
  public ValidationResultWithPayload<SampleMetadata> validateNewSample(String sampleName,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      String experimentId,
      String projectId) {

    this.assembledMetadata.set(new SampleMetadata("",
        "",
        null,
        sampleName,
        "",
        experimentId,
        -1L,
        null,
        null,
        null,
        comment));
    this.assembledMetadata.set(withComment(this.assembledMetadata.get(), comment));
    ValidationResultWithPayload<SampleMetadata> result = null;
    var experimentQuery = experimentInformationService.find(projectId,
        ExperimentId.parse(experimentId));
    if (experimentQuery.isPresent()) {
      var validationResult = validateWithExperiment(condition, analysisMethod, species, specimen,
          analyte,
          experimentQuery.get());
      result = new ValidationResultWithPayload<>(validationResult, assembledMetadata.get().copy());
    } else {
      result = new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Unknown experiment.")),
          assembledMetadata.get().copy());
    }
    return result;
  }

  private ValidationResult validateWithExperiment(String condition, String analysisMethod,
      String species, String specimen, String analyte,
      Experiment experiment) {
    var validationResult = ValidationResult.successful();
    var experimentalGroupLookupTable = conditionLookup(experiment.getExperimentalGroups());
    return validationResult.combine(validateConditions(condition, experimentalGroupLookupTable))
        .combine(validateAnalysis(analysisMethod))
        .combine(validateSpecies(species))
        .combine(validateSpecimen(specimen))
        .combine(validateAnalyte(analyte));
  }

  private ValidationResult validateConditions(String condition,
      Map<String, ExperimentalGroup> conditionsLookupTable) {
    if (conditionsLookupTable.containsKey(condition)) {
      assembledMetadata.set(new SampleMetadata(assembledMetadata.get().sampleId(),
          assembledMetadata.get().sampleCode(), assembledMetadata.get().analysisToBePerformed(),
          assembledMetadata.get().sampleName(), assembledMetadata.get().biologicalReplicate(),
          assembledMetadata.get().experimentId(), conditionsLookupTable.get(condition).id(),
          assembledMetadata.get().species(), assembledMetadata.get().specimen(),
          assembledMetadata.get().analyte(),
          assembledMetadata.get().comment()));
      return ValidationResult.successful();
    }
    return ValidationResult.withFailures(
        List.of("Unknown condition: " + condition));
  }

  private ValidationResult validateSpecies(String species) {
    var extractedTerm = PropertyConversion.extractCURIE(species);
    if (extractedTerm.isEmpty()) {
      return ValidationResult.withFailures(
          List.of("Missing CURIE in species: " + species));
    }
    var speciesLookup = speciesLookupService.findByCURI(extractedTerm.get());
    if (speciesLookup.isPresent()) {
      assembledMetadata.set(new SampleMetadata(assembledMetadata.get().sampleId(),
          assembledMetadata.get().sampleCode(), assembledMetadata.get().analysisToBePerformed(),
          assembledMetadata.get().sampleName(), assembledMetadata.get().biologicalReplicate(),
          assembledMetadata.get().experimentId(), assembledMetadata.get().experimentalGroupId(),
          OntologyTerm.from(speciesLookup.get()), assembledMetadata.get().specimen(),
          assembledMetadata.get().analyte(),
          assembledMetadata.get().comment()));

      return ValidationResult.successful();
    }
    return ValidationResult.withFailures(List.of("Unknown species: " + species));

  }

  private ValidationResult validateSpecimen(String specimen) {
    var extractedTerm = PropertyConversion.extractCURIE(specimen);
    if (extractedTerm.isEmpty()) {
      return ValidationResult.withFailures(
          List.of("Missing CURIE in specimen: " + specimen));
    }
    var specimenLookup = terminologyService.findByCurie(extractedTerm.get());
    if (specimenLookup.isPresent()) {
      assembledMetadata.set(new SampleMetadata(assembledMetadata.get().sampleId(),
          assembledMetadata.get().sampleCode(), assembledMetadata.get().analysisToBePerformed(),
          assembledMetadata.get().sampleName(), assembledMetadata.get().biologicalReplicate(),
          assembledMetadata.get().experimentId(), assembledMetadata.get().experimentalGroupId(),
          assembledMetadata.get().species(), specimenLookup.get(),
          assembledMetadata.get().analyte(),
          assembledMetadata.get().comment()));

      return ValidationResult.successful();
    }
    return ValidationResult.withFailures(List.of("Unknown specimen: " + specimen));
  }

  private ValidationResult validateAnalyte(String analyte) {
    var extractedTerm = PropertyConversion.extractCURIE(analyte);
    if (extractedTerm.isEmpty()) {
      return ValidationResult.withFailures(
          List.of("Missing CURIE in analyte: " + analyte));
    }
    var analyteLookup = terminologyService.findByCurie(extractedTerm.get());
    if (analyteLookup.isPresent()) {
      assembledMetadata.set(new SampleMetadata(assembledMetadata.get().sampleId(),
          assembledMetadata.get().sampleCode(), assembledMetadata.get().analysisToBePerformed(),
          assembledMetadata.get().sampleName(), assembledMetadata.get().biologicalReplicate(),
          assembledMetadata.get().experimentId(), assembledMetadata.get().experimentalGroupId(),
          assembledMetadata.get().species(), assembledMetadata.get().specimen(),
          analyteLookup.get(),
          assembledMetadata.get().comment()));

      return ValidationResult.successful();
    }
    return ValidationResult.withFailures(List.of("Unknown analyte: " + analyte));
  }

  private ValidationResult validateAnalysis(String analysisMethod) {
    var analysisMethodQuery = AnalysisMethod.forAbbreviation(
        analysisMethod);
    if (analysisMethodQuery.isEmpty()) {
      return ValidationResult.withFailures(
          List.of("Unknown analysis: " + analysisMethod));
    }
    assembledMetadata.set(new SampleMetadata(assembledMetadata.get().sampleId(),
        assembledMetadata.get().sampleCode(), analysisMethodQuery.get(),
        assembledMetadata.get().sampleName(), assembledMetadata.get().biologicalReplicate(),
        assembledMetadata.get().experimentId(), assembledMetadata.get().experimentalGroupId(),
        assembledMetadata.get().species(), assembledMetadata.get().specimen(),
        assembledMetadata.get().analyte(),
        assembledMetadata.get().comment()));

    return ValidationResult.successful();
  }

  /**
   * Validates the metadata for a sample that has previously been registered. A registered sample
   * has a sample code (sample id to the user) and an internal technical sample id.
   * <p>
   * The method verifies the existence of a sample with the provided sample code and resolves it to
   * the matching internal sample id.
   * <p>
   * All other validation steps are equal to a
   * {@link SampleValidation#validateNewSample(String, String, String, String, String, String, String, String, String)} call.
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
  public ValidationResultWithPayload<SampleMetadata> validateExistingSample(String sampleCode, String condition,
      String species, String specimen, String analyte, String analysisMethod, String comment,
      String experimentId,
      String projectId) {
    assembledMetadata.set(sampleMetadataWithExperimentId(experimentId));
    assembledMetadata.set(withComment(assembledMetadata.get(), comment));
    if (sampleCode.isBlank()) {
      return new ValidationResultWithPayload<>(
          ValidationResult.withFailures(List.of("Missing sample id.")),
          assembledMetadata.get().copy());
    }
    var result = sampleInformationService.findSampleId(
        SampleCode.create(sampleCode));
    if (result.isEmpty()) {
      return new ValidationResultWithPayload<>(ValidationResult.withFailures(
          List.of("Unknown sample id: " + sampleCode)), assembledMetadata.get().copy());
    }
    return validateExistingSample(sampleCode, condition, species, specimen, analyte, analysisMethod,
        comment,
        experimentId,
        projectId);
  }

  private static SampleMetadata sampleMetadataWithExperimentId(String experimentId) {
    return new SampleMetadata("", "",
        null, "", "", experimentId, -1L, null, null, null, "");
  }

  private static SampleMetadata withComment(SampleMetadata sampleMetadata, String comment) {
    return new SampleMetadata(sampleMetadata.sampleId(),
        sampleMetadata.sampleCode(),
        sampleMetadata.analysisToBePerformed(),
        sampleMetadata.sampleName(),
        sampleMetadata.biologicalReplicate(),
        sampleMetadata.experimentId(),
        sampleMetadata.experimentalGroupId(),
        sampleMetadata.species(),
        sampleMetadata.specimen(),
        sampleMetadata.analyte(),
        comment);
  }
}
