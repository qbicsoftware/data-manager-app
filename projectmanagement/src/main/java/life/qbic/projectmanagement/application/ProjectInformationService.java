package life.qbic.projectmanagement.application;

import java.util.ArrayList;
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
   * TODO
   *
   * @param projectId
   * @param species
   */
  public void addSpeciesToActiveExperiment(String projectId, Species... species) {
    ProjectId id = ProjectId.parse(projectId);
    Optional<Project> optionalProject = projectRepository.find(id);

    optionalProject.ifPresentOrElse(
        project -> {
          loadActiveExperimentForProject(project)
              .ifPresentOrElse(
                  activeExperiment -> {
                    activeExperiment.addSpecies(species);
                    experimentRepository.update(activeExperiment);
                  },
                  () -> {
                    Experiment experiment = Experiment.createForProject(project, List.of(),
                        List.of(), List.of(species));
                    experimentRepository.add(experiment);
                    project.linkExperiment(experiment.experimentId());
                    projectRepository.update(project);
                  });
        },
        () -> {
          throw new RuntimeException("There is no project with id " + id.value());
        }
    );
  }

  /**
   * TODO
   *
   * @param projectId
   * @param specimens
   */
  public void addSpecimenToActiveExperiment(String projectId, Specimen... specimens) {
    ProjectId id = ProjectId.parse(projectId);
    Optional<Project> optionalProject = projectRepository.find(id);

    optionalProject.ifPresentOrElse(
        project -> {
          loadActiveExperimentForProject(project)
              .ifPresentOrElse(
                  activeExperiment -> {
                    activeExperiment.addSpecimens(specimens);
                    experimentRepository.update(activeExperiment);
                  },
                  () -> {
                    Experiment experiment = Experiment.createForProject(project, List.of(),
                        List.of(specimens), List.of());
                    experimentRepository.add(experiment);
                    project.linkExperiment(experiment.experimentId());
                    projectRepository.update(project);
                  });
        },
        () -> {
          throw new RuntimeException("There is no project with id " + id.value());
        }
    );
  }

  /**
   * TODO
   *
   * @param projectId
   * @param analytes
   */
  public void addAnalyteToActiveExperiment(String projectId, Analyte... analytes) {
    ProjectId id = ProjectId.parse(projectId);
    Optional<Project> optionalProject = projectRepository.find(id);

    optionalProject.ifPresentOrElse(
        project -> {
          loadActiveExperimentForProject(project)
              .ifPresentOrElse(
                  activeExperiment -> {
                    activeExperiment.addAnalytes(analytes);
                    experimentRepository.update(activeExperiment);
                  },
                  () -> {
                    Experiment experiment = Experiment.createForProject(project, List.of(analytes),
                        List.of(), List.of());
                    experimentRepository.add(experiment);
                    project.linkExperiment(experiment.experimentId());
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
   * TODO analytes for active experiment or else empty list
   *
   * @param project
   * @return
   */
  public Collection<Analyte> getAnalytesOfActiveExperiment(Project project) {
    return loadActiveExperimentForProject(project).map(Experiment::getAnalytes).orElse(List.of());
  }

  /**
   * TODO
   *
   * @param project
   * @return
   */
  public Collection<Species> getSpeciesOfActiveExperiment(Project project) {
    return loadActiveExperimentForProject(project).map(Experiment::getSpecies).orElse(List.of());

  }

  /**
   * TODO
   *
   * @param project
   * @return
   */
  public Collection<Specimen> getSpecimensOfActiveExperiment(Project project) {
    return loadActiveExperimentForProject(project).map(Experiment::getSpecimens).orElse(List.of());

  }
}
