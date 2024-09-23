package life.qbic.projectmanagement.application.sample;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
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
  private static Map<String, Condition> conditionLookup(List<Condition> conditions) {
    return conditions.stream()
        .collect(Collectors.toMap(PropertyConversion::toString, Function.identity()));
  }

  /**
   * Validates metadata for a not yet registered sample. The validation does not look for any sample
   * id and no sample information lookups are done in this case.
   * <p>
   * If the client wants to validate the sample id as well, please refer to
   * {@link SampleValidation#validateExistingSample(SampleMetadata, String, String)}
   *
   * @param sampleMetadata the sample metadata to validate
   * @param experimentId   the experiment id of the experiment the sample belongs to
   * @param projectId      the project id of project the experiment belongs to
   * @return the report of the validation
   * @since 1.5.0
   */
  public ValidationResult validateNewSample(SampleMetadata sampleMetadata, String experimentId,
      String projectId) {
    var experimentQuery = experimentInformationService.find(projectId,
        ExperimentId.parse(experimentId));
    if (experimentQuery.isPresent()) {
      return validateWithExperiment(sampleMetadata, experimentQuery.get());
    } else {
      return ValidationResult.withFailures(1, List.of("Unknown experiment."));
    }
  }

  private ValidationResult validateWithExperiment(SampleMetadata sampleMetadata,
      Experiment experiment) {
    var validationResult = ValidationResult.successful(0);
    var conditionsLookupTable = conditionLookup(experiment.getExperimentalGroups().stream().map(
        ExperimentalGroup::condition).toList());
    return validationResult.combine(validateConditions(sampleMetadata, conditionsLookupTable))
        .combine(validateAnalysis(sampleMetadata))
        .combine(validateSpecies(sampleMetadata))
        .combine(validateSpecimen(sampleMetadata)).combine(validateAnalyte(sampleMetadata));
  }

  private ValidationResult validateConditions(SampleMetadata sampleMetadata,
      Map<String, Condition> conditionsLookupTable) {
    if (conditionsLookupTable.containsKey(sampleMetadata.condition())) {
      return ValidationResult.successful(1);
    }
    return ValidationResult.withFailures(0,
        List.of("Unknown condition: " + sampleMetadata.condition()));
  }

  private ValidationResult validateSpecies(SampleMetadata sampleMetadata) {
    var extractedTerm = PropertyConversion.extractCURIE(sampleMetadata.species());
    if (extractedTerm.isEmpty()) {
      return ValidationResult.withFailures(1,
          List.of("Missing CURIE in species: " + sampleMetadata.species()));
    }
    var speciesLookup = speciesLookupService.findByCURI(extractedTerm.get());
    if (speciesLookup.isPresent()) {
      return ValidationResult.successful(1);
    }
    return ValidationResult.withFailures(1, List.of("Unknown species: " + extractedTerm.get()));

  }

  private ValidationResult validateSpecimen(SampleMetadata sampleMetadata) {
    var extractedTerm = PropertyConversion.extractCURIE(sampleMetadata.specimen());
    if (extractedTerm.isEmpty()) {
      return ValidationResult.withFailures(1,
          List.of("Missing CURIE in specimen: " + sampleMetadata.species()));
    }
    var speciesLookup = terminologyService.findByCurie(extractedTerm.get());
    if (speciesLookup.isPresent()) {
      return ValidationResult.successful(1);
    }
    return ValidationResult.withFailures(1, List.of("Unknown specimen: " + extractedTerm.get()));
  }

  private ValidationResult validateAnalyte(SampleMetadata sampleMetadata) {
    var extractedTerm = PropertyConversion.extractCURIE(sampleMetadata.specimen());
    if (extractedTerm.isEmpty()) {
      return ValidationResult.withFailures(1,
          List.of("Missing CURIE in analyte: " + sampleMetadata.species()));
    }
    var speciesLookup = terminologyService.findByCurie(extractedTerm.get());
    if (speciesLookup.isPresent()) {
      return ValidationResult.successful(1);
    }
    return ValidationResult.withFailures(1, List.of("Unknown analyte: " + extractedTerm.get()));
  }

  private ValidationResult validateAnalysis(SampleMetadata sampleMetadata) {
    var analysisMethodQuery = AnalysisMethod.forAbbreviation(
        sampleMetadata.analysisToBePerformed());
    if (analysisMethodQuery.isEmpty()) {
      return ValidationResult.withFailures(1,
          List.of("Unknown analysis: " + sampleMetadata.analysisToBePerformed()));
    }
    return ValidationResult.successful(1);
  }

  /**
   * Validates the metadata for a sample that has previously been registered. A registered sample
   * has a sample code (sample id to the user) and an internal technical sample id.
   * <p>
   * The method verifies the existence of a sample with the provided sample code and resolves it to
   * the matching internal sample id.
   * <p>
   * All other validation steps are equal to a
   * {@link SampleValidation#validateNewSample(SampleMetadata, String, String)} call.
   *
   * @param sampleMetadata the sample metadata with a sample code to do the lookup
   * @param experimentId the experiment the sample belongs to
   * @param projectId the project the sample belongs to
   * @return a {@link ValidationResult} with detailed information about the validation
   * @since 1.5.0
   */
  public ValidationResult validateExistingSample(SampleMetadata sampleMetadata, String experimentId,
      String projectId) {
    if (sampleMetadata.getSampleCode().isEmpty()) {
      return ValidationResult.withFailures(1, List.of("Missing sample id."));
    }
    var result = sampleInformationService.findSampleId(
        SampleCode.create(sampleMetadata.sampleCode()));
    if (result.isEmpty()) {
      return ValidationResult.withFailures(1,
          List.of("Unknown sample id: " + sampleMetadata.sampleCode()));
    }
    return validateNewSample(sampleMetadata, experimentId, projectId);
  }


}
