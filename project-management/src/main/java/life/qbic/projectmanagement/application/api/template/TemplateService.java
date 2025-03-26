package life.qbic.projectmanagement.application.api.template;

import java.util.Arrays;
import java.util.Objects;
import life.qbic.projectmanagement.application.api.AsyncProjectService.UnsupportedMimeTypeException;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.SampleRegistration;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.PropertyConversion;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service("templateServiceV2")
public class TemplateService {

  private final ExperimentInformationService experimentService;
  private final SampleInformationService sampleService;
  private final ConfoundingVariableService confVariableService;
  private final TemplateProvider templateProvider;

  @Autowired
  public TemplateService(ExperimentInformationService experimentService,
      SampleInformationService sampleService,
      ConfoundingVariableService confVariableService, TemplateProvider templateProvider) {
    this.experimentService = Objects.requireNonNull(experimentService);
    this.sampleService = Objects.requireNonNull(sampleService);
    this.confVariableService = confVariableService;
    this.templateProvider = Objects.requireNonNull(templateProvider);
  }

  /**
   * Creates a {@link DigitalObject} that contains a template in the provided {@link MimeType} that
   * can be used to register one or more sample batches for an experiment.
   * <p>
   * The workbook contains two sheets. The first one is for the user input and contains cell with
   * data-validation, e.g. a list of available conditions in the experiment.
   * <p>
   * In total, the template provides data validation for the properties:
   *
   * <ul>
   *   <li>Species</li>
   *   <li>Specimen</li>
   *   <li>Analyte</li>
   *   <li>Condition</li>
   *   <li>Analysis to perform</li>
   * </ul>
   *
   * @param projectId    the project id of the project that contains the experiment for which the
   *                     template shall be generated
   * @param experimentId the experiment id of the experiment to create the template for
   * @return a pre-configured template workbook
   * @throws NoSuchExperimentException    if no experiment with the provided id can be found.
   * @throws UnsupportedMimeTypeException if there is no support for the requested {@link MimeType}
   * @since 1.5.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public DigitalObject sampleRegistrationTemplate(String projectId, String experimentId,
      MimeType type) {
    if (!isSupportedMimeType(type)) {
      throw new UnsupportedMimeTypeException("Unsupported mime type: " + type);
    }
    return generateSampleRegistrationTemplate(projectId, experimentId);
  }

  private DigitalObject generateSampleRegistrationTemplate(String projectId, String experimentId) {
    var experiment = experimentService.find(projectId, ExperimentId.parse(experimentId))
        .orElseThrow(
            NoSuchExperimentException::new);
    var conditions = experiment.getExperimentalGroups().stream().map(ExperimentalGroup::condition)
        .map(
            PropertyConversion::toString).toList();
    var species = experiment.getSpecies().stream().map(PropertyConversion::toString).toList();
    var specimen = experiment.getSpecimens().stream().map(PropertyConversion::toString).toList();
    var analytes = experiment.getAnalytes().stream().map(PropertyConversion::toString).toList();
    var analysisMethods = Arrays.stream(AnalysisMethod.values()).map(AnalysisMethod::abbreviation)
        .toList();
    var confoundingVariables = confVariableService.listConfoundingVariablesForExperiment(
            projectId, new ExperimentReference(experimentId)).stream()
        .map(it -> new ConfoundingVariableInformation(it.id(), it.variableName()))
        .toList();
    return templateProvider.getTemplate(new SampleRegistration(
        analysisMethods,
        conditions,
        analytes,
        species,
        specimen,
        confoundingVariables
    ));
  }

  private boolean isSupportedMimeType(MimeType mimeType) {
    return mimeType.equalsTypeAndSubtype(templateProvider.providedMimeType());
  }


  static class NoSuchExperimentException extends RuntimeException {

    public NoSuchExperimentException() {
      super();
    }
  }
}
