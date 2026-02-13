package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.sample;

import java.util.List;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;

/**
 * Sample Template Factory.
 * <p>
 * Creates {@link WorkbookFactory} for different scenarios for sample information management.
 *
 * @since 1.10.0
 */
public class SampleTemplateFactory {

  /**
   * Requests a {@link WorkbookFactory} for sample registrations.
   *
   * @param analysisMethods      the analysis methods planned
   * @param conditions           the conditions available from the experimental design
   * @param analytes             the analytes planned for measurement
   * @param species              the species the sample derives
   * @param specimen             the specimen the sample derives
   * @param confoundingVariables confounding variables known to the experiment
   * @return a {@link WorkbookFactory} that handles the sample registration template
   * @since 1.10.0
   */
  public WorkbookFactory forRegistration(List<String> analysisMethods, List<String> conditions,
      List<String> analytes, List<String> species,
      List<String> specimen, List<ConfoundingVariableInformation> confoundingVariables) {
    return new SampleRegisterFactory(
        analysisMethods,
        conditions,
        analytes,
        species,
        specimen,
        confoundingVariables);
  }

  /**
   * @param samples                   the samples to be updated
   * @param analysisMethods           the analysis methods planned
   * @param conditions                the conditions available from the experimental design
   * @param analytes                  the analytes planned for measurement
   * @param species                   the species the sample derives
   * @param specimen                  the specimen the sample derives
   * @param confoundingVariables      confounding variables known to the experiment
   * @param confoundingVariableLevels the available levels of confounding variables
   * @return a {@link WorkbookFactory} that handles the sample update template
   * @since 1.10.0
   */
  public WorkbookFactory forUpdate(List<Sample> samples, List<String> analysisMethods,
      List<String> conditions, List<String> analytes, List<String> species,
      List<String> specimen, List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariableInformation> confoundingVariables,
      List<ConfoundingVariableLevel> confoundingVariableLevels) {
    return new SampleUpdateFactory(
        samples,
        analysisMethods,
        conditions,
        analytes,
        species,
        specimen,
        experimentalGroups,
        confoundingVariables,
        confoundingVariableLevels
    );
  }

  /**
   * @param samples                   the target samples to display the information for
   * @param analysisMethods           the analysis methods planned
   * @param conditions                the conditions available from the experimental design
   * @param analytes                  the analytes planned for measurement
   * @param species                   the species the sample derives
   * @param specimen                  the specimen the sample derives
   * @param confoundingVariables      confounding variables known to the experiment
   * @param confoundingVariableLevels the available levels of confounding variables
   * @return a {@link WorkbookFactory} that handles the sample information preparation
   * @since 1.10.0
   */
  public WorkbookFactory forInformation(List<Sample> samples, List<String> analysisMethods,
      List<String> conditions, List<String> analytes, List<String> species,
      List<String> specimen, List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariableInformation> confoundingVariables,
      List<ConfoundingVariableLevel> confoundingVariableLevels) {
    return new SampleInformationFactory(
        samples,
        analysisMethods,
        conditions,
        analytes,
        species,
        specimen,
        experimentalGroups,
        confoundingVariables,
        confoundingVariableLevels
    );
  }
}
