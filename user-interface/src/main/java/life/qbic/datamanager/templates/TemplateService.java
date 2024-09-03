package life.qbic.datamanager.templates;

import java.util.Objects;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <b>Template Service</b>
 * <p>
 * Service that enables access to various template generation methods to support tasks such as
 * sample batch registration and sample batch update.
 *
 * @since 1.5.0
 */
public class TemplateService {


  private final ExperimentInformationService experimentInfoService;

  public TemplateService(ExperimentInformationService experimentInfoService) {
    this.experimentInfoService = Objects.requireNonNull(experimentInfoService);
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

  private XSSFWorkbook createWorkbookFromExperiment(Experiment experiment) {
    // TODO Load specimen, species, analyte, condition information
    // TODO Load analysis to perform enums
    throw new RuntimeException("Not yet implemented");
    //return SampleBatchTemplate.createRegistrationTemplate();
  }

  public static class NoSuchExperimentException extends RuntimeException {

  }

}
