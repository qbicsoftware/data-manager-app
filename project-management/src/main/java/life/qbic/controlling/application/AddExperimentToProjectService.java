package life.qbic.controlling.application;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.controlling.domain.model.project.Project;
import life.qbic.controlling.domain.model.project.ProjectId;
import life.qbic.controlling.domain.model.experiment.Experiment;
import life.qbic.controlling.domain.model.experiment.ExperimentId;
import life.qbic.controlling.domain.model.experiment.vocabulary.Analyte;
import life.qbic.controlling.domain.model.experiment.vocabulary.Species;
import life.qbic.controlling.domain.model.experiment.vocabulary.Specimen;
import life.qbic.controlling.domain.repository.ProjectRepository;
import life.qbic.controlling.domain.repository.ProjectRepository.ProjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
   * @return a result containing the id of the added experiment, a failure result otherwise
   */
  public Result<ExperimentId, RuntimeException> addExperimentToProject(ProjectId projectId,
      String experimentName,
      List<Species> species,
      List<Specimen> specimens,
      List<Analyte> analytes) {
      requireNonNull(projectId, "project id must not be null during experiment creation");
      if (experimentName.isBlank()) {
        //ToDo Add Iterator for multiple experiments?
        experimentName = "Unnamed Experiment";
      }
      if (CollectionUtils.isEmpty(species)) {
        throw new ApplicationException(ErrorCode.NO_SPECIES_DEFINED,
            ErrorParameters.of(species));
      }
      if (CollectionUtils.isEmpty(specimens)) {
        throw new ApplicationException(ErrorCode.NO_SPECIMEN_DEFINED,
            ErrorParameters.of(specimens));
      }
      if (CollectionUtils.isEmpty(analytes)) {
        throw new ApplicationException(ErrorCode.NO_ANALYTE_DEFINED,
            ErrorParameters.of(analytes));
      }
      Optional<Project> optionalProject = projectRepository.find(projectId);
      if (optionalProject.isEmpty()) {
        return Result.fromError(new ProjectNotFoundException());
      }
      Project project = optionalProject.get();
      return Result.<Experiment, RuntimeException>fromValue(
              Experiment.create(experimentName))
          .onValue(exp -> exp.addAnalytes(analytes))
          .onValue(exp -> exp.addSpecies(species))
          .onValue(exp -> exp.addSpecimens(specimens))
          .onValue(experiment -> {
            project.addExperiment(experiment);
            projectRepository.update(project);
          })
          .map(Experiment::experimentId);
  }

}