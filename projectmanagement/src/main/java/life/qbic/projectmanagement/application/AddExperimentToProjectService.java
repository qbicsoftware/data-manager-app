package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.application.commons.tmp.Either;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository.ProjectNotFoundException;
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
      List<Analyte> analytes,
      List<Species> species,
      List<Specimen> specimens) {

      requireNonNull(projectId, "project id must not be null during experiment creation");
      requireNonNull(experimentName, "experiment name must not be null during experiment creation");
      if (CollectionUtils.isEmpty(analytes)) {
        throw new ProjectManagementException(ErrorCode.NO_ANALYTE_DEFINED,
            ErrorParameters.of(analytes));
      }
    if (CollectionUtils.isEmpty(species)) {
      throw new ProjectManagementException(ErrorCode.NO_SPECIES_DEFINED,
          ErrorParameters.of(species));
    }
    if (CollectionUtils.isEmpty(specimens)) {
      throw new ProjectManagementException(ErrorCode.NO_SPECIMEN_DEFINED,
          ErrorParameters.of(specimens));
    }

    Optional<Project> optionalProject = projectRepository.find(projectId);
    if (optionalProject.isEmpty()) {
      return Result.failure(new ProjectNotFoundException());
    }
    Project project = optionalProject.get();

    Either<ExperimentId, RuntimeException> either = Either.<Experiment, RuntimeException>fromValue(
            Experiment.create(experimentName))
        .onValue(exp -> exp.addAnalytes(analytes))
        .onValue(exp -> exp.addSpecies(species))
        .onValue(exp -> exp.addSpecimens(specimens))
        .onValue(experiment -> {
          project.addExperiment(experiment);
          projectRepository.update(project);
        })
        .map(Experiment::experimentId);

    return either.fold(Result::success, Result::failure);
  }

}
