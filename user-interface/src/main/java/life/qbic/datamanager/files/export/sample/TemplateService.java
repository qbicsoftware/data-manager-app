package life.qbic.datamanager.files.export.sample;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariable;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.PropertyConversion;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import org.apache.poi.ss.usermodel.Workbook;
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
  private final ConfoundingVariableService confoundingVariableService;

  @Autowired
  public TemplateService(ExperimentInformationService experimentInfoService,
      SampleInformationService sampleInformationService,
      ConfoundingVariableService confoundingVariableService) {
    this.experimentInfoService = Objects.requireNonNull(experimentInfoService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.confoundingVariableService = confoundingVariableService;
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
  public Workbook sampleBatchRegistrationXLSXTemplate(String projectId, String experimentId)
      throws NoSuchExperimentException {
    var experiment = experimentInfoService.find(projectId, ExperimentId.parse(experimentId))
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
    List<ConfoundingVariable> confoundingVariables = confoundingVariableService.listConfoundingVariablesForExperiment(
            projectId, new ExperimentReference(experimentId)).stream()
        .map(it -> new ConfoundingVariable(it.id(), it.variableName()))
        .toList();
    return SampleWorkbooks.createRegistrationWorkbook(analysisMethods, conditions, analytes,
        species, specimen, confoundingVariables);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Workbook sampleBatchUpdateXLSXTemplate(BatchId batchId, String projectId,
      String experimentId)
      throws NoSuchExperimentException, SampleSearchException {
    var experiment = experimentInfoService.find(projectId, ExperimentId.parse(experimentId))
        .orElseThrow(
            NoSuchExperimentException::new);
    var samples = sampleInformationService.retrieveSamplesForExperiment(
        ExperimentId.parse(experimentId));
    List<ConfoundingVariable> confoundingVariables = confoundingVariableService.listConfoundingVariablesForExperiment(
            projectId, new ExperimentReference(experimentId))
        .stream()
        .map(it -> new ConfoundingVariable(it.id(), it.variableName()))
        .toList();
    List<ConfoundingVariableLevel> confoundingVariableLevels = confoundingVariableService.listLevelsForVariables(
        projectId,
        confoundingVariables.stream().map(ConfoundingVariable::variableReference).toList());
    samples.onError(responseCode -> {
      throw new SampleSearchException();
    });
    var samplesInBatch = samples.getValue().stream()
        .filter(sample -> sample.assignedBatch().equals(batchId))
        .toList();
    var conditions = experiment.getExperimentalGroups().stream()
        .map(ExperimentalGroup::condition)
        .map(PropertyConversion::toString).toList();
    var experimentalGroups = experiment.getExperimentalGroups();
    var species = experiment.getSpecies().stream().map(PropertyConversion::toString).toList();
    var specimen = experiment.getSpecimens().stream().map(PropertyConversion::toString).toList();
    var analytes = experiment.getAnalytes().stream().map(PropertyConversion::toString).toList();
    var analysisMethods = Arrays.stream(AnalysisMethod.values())
        .map(AnalysisMethod::abbreviation)
        .toList();
    return SampleWorkbooks.createEditWorkbook(samplesInBatch,
        analysisMethods,
        conditions,
        analytes,
        species,
        specimen,
        experimentalGroups,
        confoundingVariables,
        confoundingVariableLevels);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Workbook sampleBatchInformationXLSXTemplate(String projectId,
      String experimentId)
      throws NoSuchExperimentException, SampleSearchException {
    var experiment = experimentInfoService.find(projectId, ExperimentId.parse(experimentId))
        .orElseThrow(
            NoSuchExperimentException::new);
    var samples = sampleInformationService.retrieveSamplesForExperiment(
        ExperimentId.parse(experimentId));
    samples.onError(responseCode -> {
      throw new SampleSearchException();
    });
    var samplesInBatch = samples.getValue().stream().toList();
    var conditions = experiment.getExperimentalGroups().stream()
        .map(ExperimentalGroup::condition)
        .map(PropertyConversion::toString).toList();
    var experimentalGroups = experiment.getExperimentalGroups();
    var species = experiment.getSpecies().stream().map(PropertyConversion::toString).toList();
    var specimen = experiment.getSpecimens().stream().map(PropertyConversion::toString).toList();
    var analytes = experiment.getAnalytes().stream().map(PropertyConversion::toString).toList();
    var analysisMethods = Arrays.stream(AnalysisMethod.values())
        .map(AnalysisMethod::abbreviation)
        .toList();

    List<ConfoundingVariable> confoundingVariables = confoundingVariableService.listConfoundingVariablesForExperiment(
            projectId, new ExperimentReference(experimentId))
        .stream()
        .map(it -> new ConfoundingVariable(it.id(), it.variableName()))
        .toList();
    List<ConfoundingVariableLevel> confoundingVariableLevels = confoundingVariableService.listLevelsForVariables(
        projectId,
        confoundingVariables.stream().map(ConfoundingVariable::variableReference).toList());
    return SampleWorkbooks.createInformationWorkbook(samplesInBatch,
        analysisMethods,
        conditions,
        analytes,
        species,
        specimen,
        experimentalGroups,
        confoundingVariables,
        confoundingVariableLevels);
  }

  public static class NoSuchExperimentException extends RuntimeException {

  }

  public static class SampleSearchException extends RuntimeException {

  }

}
