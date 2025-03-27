package life.qbic.projectmanagement.application.api.template;

import java.util.List;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.util.MimeType;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface TemplateProvider {

  MimeType providedMimeType();

  DigitalObject getTemplate(TemplateRequest request);

  sealed interface TemplateRequest permits SampleInformation, SampleRegistration, SampleUpdate {

  }

  record SampleRegistration(
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes,
      List<String> species,
      List<String> specimen,
      List<ConfoundingVariableInformation> confoundingVariables
  ) implements TemplateRequest {

  }

  record SampleUpdate(SampleInformation information) implements TemplateRequest {

  }

  record SampleInformation(
      List<Sample> samples,
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes,
      List<String> species,
      List<String> specimen,
      List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariableInformation> confoundingVariables,
      List<ConfoundingVariableLevel> confoundingVariableLevels
  ) implements TemplateRequest {

  }

}



