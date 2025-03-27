package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

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
