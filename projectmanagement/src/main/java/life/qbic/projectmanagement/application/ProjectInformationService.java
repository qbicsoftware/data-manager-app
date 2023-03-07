package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentRepository;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic project information
 *
 * @since 1.0.0
 */
@Service
public class ProjectInformationService {

  private static final Logger log = LoggerFactory.logger(ProjectInformationService.class);
  private final ProjectPreviewLookup projectPreviewLookup;
  private final ProjectRepository projectRepository;
  private final ExperimentRepository experimentRepository;

  public ProjectInformationService(@Autowired ProjectPreviewLookup projectPreviewLookup,
      @Autowired ProjectRepository projectRepository,
      @Autowired ExperimentRepository experimentRepository) {
    this.experimentRepository = experimentRepository;
    Objects.requireNonNull(projectPreviewLookup);
    this.projectPreviewLookup = projectPreviewLookup;
    this.projectRepository = projectRepository;
  }

  /**
   * Queries {@link ProjectPreview}s with a provided offset and limit that supports pagination.
   *
   * @param filter     the results' project title will be applied with this filter
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   * @since 1.0.0
   */
  @PostFilter("hasPermission(filterObject,'VIEW_PROJECT')")
  public List<ProjectPreview> queryPreview(String filter, int offset, int limit,
      List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<ProjectPreview> previewList = projectPreviewLookup.query(filter, offset, limit,
        sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(previewList);
  }

  private Optional<Experiment> loadActiveExperimentForProject(Project project) {
    return project.activeExperiment()
        .map(experimentId ->
            experimentRepository.find(experimentId)
                .orElseThrow(
                    () -> new RuntimeException("The active experiment does not exist anymore.")
                    // should never happen; indicates dirty removal of experiment from db
                ));
  }

  /**
   * Adds species to the active experiment of a project. If no experiment is active, a new
   * experiment is created and set as the active experiment.
   *
   * @param projectId the project for which to add the species
   * @param species   the species to add
   * @see Experiment#addSpecies(Collection)
   */
  public void addSpeciesToActiveExperiment(String projectId, Species... species) {
    if (species.length < 1) {
      return;
    }
    Arrays.stream(species).forEach(Objects::requireNonNull);

    ProjectId id = ProjectId.parse(projectId);
    Optional<Project> optionalProject = projectRepository.find(id);

    optionalProject.ifPresentOrElse(
        project -> {
          loadActiveExperimentForProject(project)
              .ifPresentOrElse(
                  activeExperiment -> {
                    activeExperiment.addSpecies(List.of(species));
                    experimentRepository.update(activeExperiment);
                  },
                  () -> {
                    Experiment experiment = Experiment.create(project.getId(), List.of(),
                        List.of(), List.of(species));
                    experimentRepository.add(experiment);
                    project.addExperiment(experiment.experimentId());
                    projectRepository.update(project);
                  });
        },
        () -> {
          throw new RuntimeException("There is no project with id " + id.value());
        }
    );
  }

  /**
   * Adds specimens to the active experiment of a project. If no experiment is active, a new experiment is created and set as the active experiment.
   *
   * @see Experiment#addSpecimens(Collection)
   *
   * @param projectId the project for which to add the species
   * @param specimens the specimens to add
   */
  public void addSpecimenToActiveExperiment(String projectId, Specimen... specimens) {
    if (specimens.length < 1) {
      return;
    }
    for (Specimen specimen : specimens) {
      Objects.requireNonNull(specimen);
    }

    ProjectId id = ProjectId.parse(projectId);
    Optional<Project> optionalProject = projectRepository.find(id);

    optionalProject.ifPresentOrElse(
        project -> {
          loadActiveExperimentForProject(project)
              .ifPresentOrElse(
                  activeExperiment -> {
                    activeExperiment.addSpecimens(List.of(specimens));
                    experimentRepository.update(activeExperiment);
                  },
                  () -> {
                    Experiment experiment = Experiment.create(project.getId(), List.of(),
                        List.of(specimens), List.of());
                    experimentRepository.add(experiment);
                    project.addExperiment(experiment.experimentId());
                    projectRepository.update(project);
                  });
        },
        () -> {
          throw new RuntimeException("There is no project with id " + id.value());
        }
    );
  }

  /**
   * Adds analytes to the active experiment of a project. If no experiment is active, a new experiment is created and set as the active experiment.
   *
   * @see Experiment#addAnalytes(Collection)
   *
   * @param projectId the project for which to add the species
   * @param analytes the analytes to add
   */
  public void addAnalyteToActiveExperiment(String projectId, Analyte... analytes) {
    if (analytes.length < 1) {
      return;
    }
    Arrays.stream(analytes).forEach(Objects::requireNonNull);

    ProjectId id = ProjectId.parse(projectId);
    Optional<Project> optionalProject = projectRepository.find(id);

    optionalProject.ifPresentOrElse(
        project -> {
          loadActiveExperimentForProject(project)
              .ifPresentOrElse(
                  activeExperiment -> {
                    activeExperiment.addAnalytes(List.of(analytes));
                    experimentRepository.update(activeExperiment);
                  },
                  () -> {
                    Experiment experiment = Experiment.create(project.getId(), List.of(analytes),
                        List.of(), List.of());
                    experimentRepository.add(experiment);
                    project.addExperiment(experiment.experimentId());
                    projectRepository.update(project);
                  });
        },
        () -> {
          throw new RuntimeException("There is no project with id " + id.value());
        }
    );
  }

  @PostAuthorize("hasPermission(returnObject.get(),'VIEW_PROJECT')")
  public Optional<Project> find(ProjectId projectId) {
    log.debug("Search for project with id: " + projectId.toString());
    return projectRepository.find(projectId);
  }

  public void updateTitle(String projectId, String newTitle) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectTitle projectTitle = ProjectTitle.of(newTitle);
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.updateTitle(projectTitle);
      projectRepository.update(p);
    });
  }

  public void manageProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.setProjectManager(personReference);
      projectRepository.update(p);
    });
  }

  public void investigateProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.setPrincipalInvestigator(personReference);
      projectRepository.update(p);
    });
  }

  public void setResponsibility(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.setResponsiblePerson(personReference);
      projectRepository.update(p);
    });
  }

  public void describeExperimentalDesign(String projectId, String experimentalDesign) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ExperimentalDesignDescription experimentalDesignDescription = ExperimentalDesignDescription.create(
        experimentalDesign);
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.describeExperimentalDesign(experimentalDesignDescription);
      projectRepository.update(p);
    });
  }

  public void stateObjective(String projectId, String objective) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.stateObjective(projectObjective);
      projectRepository.update(p);
    });
  }


  /**
   * Retrieve all analytes of the active experiment. If no experiment is active, returns an empty
   * list.
   *
   * @param projectId the project the experiment belongs to
   * @return a collection of analytes in the active experiment. If no experiment is active, returns
   * an empty list.
   */
  public Collection<Analyte> getAnalytesOfActiveExperiment(ProjectId projectId) {
    return projectRepository.find(projectId)
        .map(project ->
            loadActiveExperimentForProject(project).map(Experiment::getAnalytes)
                .orElse(List.of()))
        .orElseThrow(() -> new IllegalArgumentException("Project could not be retrieved."));
  }

  /**
   * Retrieve all species of the active experiment. If no experiment is active, returns an empty
   * list.
   *
   * @param projectId the project the experiment belongs to
   * @return a collection of species in the active experiment. If no experiment is active, returns
   * an empty list.
   */
  public Collection<Species> getSpeciesOfActiveExperiment(ProjectId projectId) {
    return projectRepository.find(projectId)
        .map(project ->
            loadActiveExperimentForProject(project).map(Experiment::getSpecies)
                .orElse(List.of()))
        .orElseThrow(() -> new IllegalArgumentException("Project could not be retrieved."));

  }

  /**
   * Retrieve all specimen of the active experiment. If no experiment is active, returns an empty
   * list.
   *
   * @param projectId the project the experiment belongs to
   * @return a collection of specimen in the active experiment. If no experiment is active, returns
   * an empty list.
   */
  public Collection<Specimen> getSpecimensOfActiveExperiment(ProjectId projectId) {
    return projectRepository.find(projectId)
        .map(project ->
            loadActiveExperimentForProject(project).map(Experiment::getSpecimens)
                .orElse(List.of()))
        .orElseThrow(() -> new IllegalArgumentException("Project could not be retrieved."));

  }
}
