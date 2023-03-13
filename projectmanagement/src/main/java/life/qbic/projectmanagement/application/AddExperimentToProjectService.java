package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;

import java.util.List;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository.ProjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An application service that adds an experiment to a project.
 *
 * @since 1.0.0
 */
@Service
public class AddExperimentToProjectService {

  private final ProjectRepository projectRepository;

  public AddExperimentToProjectService(
      @Autowired ProjectRepository projectRepository) {

    this.projectRepository = projectRepository;
  }

  /**
   * Creates a new experiment with the information provided and adds it to the project.
   *
   * @param projectId      the project for which to add the experiment
   * @param experimentName the name of the experiment
   * @param analytes       analytes associated with the experiment
   * @param species        species associated with the experiment
   * @param specimens      specimens associated with the experiment
   */
  public void addExperimentToProject(ProjectId projectId, String experimentName,
      List<Analyte> analytes, List<Species> species, List<Specimen> specimens) {
    try {
      requireNonNull(projectId);
      requireNonNull(experimentName);
      requireNonNull(analytes);
      requireNonNull(species);
      requireNonNull(specimens);
      Project project = projectRepository.find(projectId)
          .orElseThrow(ProjectNotFoundException::new);
      Experiment experiment = Experiment.create(experimentName, analytes, specimens, species);
      project.addExperiment(experiment);
      projectRepository.update(project);
    } catch (Exception e) {
      throw new ProjectManagementException(e.getMessage(), e);
    }
  }

}
