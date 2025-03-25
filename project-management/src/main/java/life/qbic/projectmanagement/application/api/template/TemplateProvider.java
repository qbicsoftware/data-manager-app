package life.qbic.projectmanagement.application.api.template;

import java.util.List;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.TemplateRequest;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
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

  sealed interface TemplateRequest permits SampleRegistration{
  }

  record SampleRegistration(
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes, List<String> species, List<String> specimen,
      List<ConfoundingVariableInformation> confoundingVariables) implements TemplateRequest {}


}



