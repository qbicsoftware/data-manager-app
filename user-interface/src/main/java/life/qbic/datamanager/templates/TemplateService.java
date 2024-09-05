package life.qbic.datamanager.templates;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.parser.PropertyToString;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * <b>Template Service</b>
 * <p>
 * Service that enables access to various template generation methods to support tasks such as
 * sample batch registration and sample batch update.
 *
 * @since 1.5.0
 */
@Service
public class TemplateService {

  private final ExperimentInformationService experimentInfoService;

  private final SampleInformationService sampleInformationService;

  @Autowired
  public TemplateService(ExperimentInformationService experimentInfoService,
      SampleInformationService sampleInformationService) {
    this.experimentInfoService = Objects.requireNonNull(experimentInfoService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

  /**
   * Creates a {@link XSSFWorkbook} that contains a template
   * {@link org.apache.poi.xssf.usermodel.XSSFSheet} that can be used to register one or more sample
   * batches for an experiment.
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
   * @throws NoSuchExperimentException if no experiment with the provided id can be found.
   * @since 1.5.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public XSSFWorkbook sampleBatchRegistrationTemplate(String projectId, String experimentId)
      throws NoSuchExperimentException {
    var experiment = experimentInfoService.find(projectId, ExperimentId.parse(experimentId))
        .orElseThrow(
            NoSuchExperimentException::new);
    return createWorkbookFromExperiment(experiment);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public XSSFWorkbook sampleBatchUpdateTemplate(String projectId, String experimentId)
      throws NoSuchExperimentException, SampleSearchException {
    var experiment = experimentInfoService.find(projectId, ExperimentId.parse(experimentId))
        .orElseThrow(
            NoSuchExperimentException::new);
    var samples = sampleInformationService.retrieveSamplesForExperiment(
        ExperimentId.parse(experimentId));
    samples.onError(responseCode -> {
      throw new SampleSearchException();
    });
    return createPrefilledWorkbookFromExperiment(experiment, samples.getValue().stream().toList());
  }

  private XSSFWorkbook createPrefilledWorkbookFromExperiment(Experiment experiment,
      List<Sample> samples) {
    var conditions = experiment.getExperimentalGroups().stream().map(ExperimentalGroup::condition)
        .map(
            PropertyToString::condition).toList();
    var experimentalGroups = experiment.getExperimentalGroups();
    var species = experiment.getSpecies().stream().map(PropertyToString::ontologyTerm).toList();
    var specimen = experiment.getSpecimens().stream().map(PropertyToString::ontologyTerm).toList();
    var analytes = experiment.getAnalytes().stream().map(PropertyToString::ontologyTerm).toList();
    var analysisMethods = Arrays.stream(AnalysisMethod.values()).map(AnalysisMethod::abbreviation)
        .toList();
    return SampleBatchTemplate.createUpdateTemplate(samples,
        conditions, species,
        specimen, analytes,
        analysisMethods, experimentalGroups);
  }

  private XSSFWorkbook createWorkbookFromExperiment(Experiment experiment) {
    var conditions = experiment.getExperimentalGroups().stream().map(ExperimentalGroup::condition)
        .map(
            PropertyToString::condition).toList();
    var species = experiment.getSpecies().stream().map(PropertyToString::ontologyTerm).toList();
    var specimen = experiment.getSpecimens().stream().map(PropertyToString::ontologyTerm).toList();
    var analytes = experiment.getAnalytes().stream().map(PropertyToString::ontologyTerm).toList();
    var analysisMethods = Arrays.stream(AnalysisMethod.values()).map(AnalysisMethod::abbreviation)
        .toList();
    return SampleBatchTemplate.createRegistrationTemplate(
        conditions, species,
        specimen, analytes,
        analysisMethods);
  }

  public static class NoSuchExperimentException extends RuntimeException {

  }

  public static class SampleSearchException extends RuntimeException {

  }

}
