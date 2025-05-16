package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentCreatedDomainEventSubscriber;
import life.qbic.projectmanagement.domain.model.OntologyTermV1;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.repository.ProjectRepository.ProjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Result<ExperimentId, RuntimeException> addExperimentToProject(ProjectId projectId,
      String experimentName,
      List<OntologyTermV1> species,
      List<OntologyTermV1> specimens,
      List<OntologyTermV1> analytes) {
      requireNonNull(projectId, "project id must not be null during experiment creation");
      if (experimentName.isBlank()) {
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
        
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new ExperimentCreatedDomainEventSubscriber(domainEventsCache));
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
        .map(Experiment::experimentId)
        .onValue(experimentId ->
            domainEventsCache.forEach(
                domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent)));
  }

}
