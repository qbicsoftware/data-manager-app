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
      return validateWithExperiment(sampleMetadata, experimentQuery.get(), projectId);
    } else {
      return ValidationResult.withFailures(1, List.of("Unknown experiment."));
    }
  }

  private ValidationResult validateWithExperiment(SampleMetadata sampleMetadata,
      Experiment experiment, String projectId) {
    var conditionsLookupTable = conditionLookup(experiment.getExperimentalGroups().stream().map(
        ExperimentalGroup::condition).toList());

    throw new RuntimeException("Not implemented yet");
  }

  public ValidationResult validateExistingSample(SampleMetadata sampleMetadata, String experimentId,
      String projectId) {
    throw new RuntimeException("Not yet implemented");
  }


}
