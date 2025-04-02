package life.qbic.projectmanagement.application.api.template;

import java.util.List;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.util.MimeType;

/**
 * Template provider.
 * <p>
 * Templates can be used to provide information for various purposes in the application (e.g. sample
 * registration, update tasks, batch information display, etc.).
 * <p>
 * A template provider prepares these templates to comply with a certain {@link MimeType}.
 *
 * @since 1.10.0
 */
public interface TemplateProvider {

  MimeType providedMimeType();

  /**
   * Requests a template as {@link DigitalObject}.
   * <p>
   * Implementations must guarantee that all known implementations of {@link TemplateRequest} are
   * handled.
   *
   * @param request the template request
   * @return a {@link DigitalObject} that provides the actual template
   * @since 1.10.0
   */
  DigitalObject getTemplate(TemplateRequest request);

  /**
   * A template request is the general typification for requests for different kind of template
   * generation.
   *
   * @since 1.10.0
   */
  sealed interface TemplateRequest permits SampleInformation, SampleRegistration, SampleUpdate {

  }

  /**
   * Information container for sample registration template requests.
   *
   * @param analysisMethods
   * @param conditions
   * @param analytes
   * @param species
   * @param specimen
   * @param confoundingVariables
   * @since 1.10.0
   */
  record SampleRegistration(
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes,
      List<String> species,
      List<String> specimen,
      List<ConfoundingVariableInformation> confoundingVariables
  ) implements TemplateRequest {

  }

  /**
   * Information container for sample update template requests.
   *
   * @param information the sample information to prepare in the template for update
   * @since 1.10.0
   */
  record SampleUpdate(SampleInformation information) implements TemplateRequest {

  }

  /**
   * Information container for sample information template requests.
   *
   * @param samples
   * @param analysisMethods
   * @param conditions
   * @param analytes
   * @param species
   * @param specimen
   * @param experimentalGroups
   * @param confoundingVariables
   * @param confoundingVariableLevels
   * @since 1.10.0
   */
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



