package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public class AddExperimentToProjectService {

  private final ExperimentRepository experimentRepository;
  private final ProjectRepository projectRepository;
  private static final Logger log = logger(AddExperimentToProjectService.class);

  public AddExperimentToProjectService(
      @Autowired ExperimentRepository experimentRepository,
      @Autowired ProjectRepository projectRepository) {

    this.experimentRepository = experimentRepository;
    this.projectRepository = projectRepository;
  }

  public void addExperimentToProject(ProjectId projectId, String name,
      List<Analyte> analytes, List<Species> species, List<Specimen> specimens) {
    requireNonNull(projectId);
    requireNonNull(name);
    requireNonNull(analytes);
    requireNonNull(species);
    requireNonNull(specimens);
    // load project
    Project project = projectRepository.find(projectId).orElseThrow();// todo throw what?
    // create experiment
    Experiment experiment = Experiment.create(name, analytes, specimens, species);
    project.addExperiment(experiment);
    projectRepository.update(project);
  }

}
