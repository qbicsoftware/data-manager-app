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

  private Project loadProject(ProjectId projectId) {
    return projectRepository.find(projectId).orElseThrow(() -> new ProjectManagementException(
            "Project with id" + projectId.toString() + "does not exit anymore")
        // should never happen; indicates dirty removal of project from db
    );
  }

  private Experiment loadActiveExperimentForProject(Project project) {
    return experimentRepository.find(project.activeExperiment()).orElseThrow(
        () -> new ProjectManagementException("The active experiment does not exist anymore.")
        // should never happen; indicates dirty removal of experiment from db
    );
  }

  /**
   * Adds species to the active experiment of a project.
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
    Project project = loadProject(id);
    Experiment activeExperiment = loadActiveExperimentForProject(project);
    activeExperiment.addSpecies(List.of(species));
    experimentRepository.update(activeExperiment);
  }

  /**
   * Adds specimens to the active experiment of a project. I
   *
   * @param projectId the project for which to add the species
   * @param specimens the specimens to add
   * @see Experiment#addSpecimens(Collection)
   */
  public void addSpecimenToActiveExperiment(String projectId, Specimen... specimens) {
    if (specimens.length < 1) {
      return;
    }
    for (Specimen specimen : specimens) {
      Objects.requireNonNull(specimen);
    }
    ProjectId id = ProjectId.parse(projectId);
    Project project = loadProject(id);
    Experiment activeExperiment = loadActiveExperimentForProject(project);
    activeExperiment.addSpecimens(List.of(specimens));
    experimentRepository.update(activeExperiment);
  }

  /**
   * Adds analytes to the active experiment of a project.
   *
   * @param projectId the project for which to add the species
   * @param analytes  the analytes to add
   * @see Experiment#addAnalytes(Collection)
   */
  public void addAnalyteToActiveExperiment(String projectId, Analyte... analytes) {
    if (analytes.length < 1) {
      return;
    }
    Arrays.stream(analytes).forEach(Objects::requireNonNull);
    ProjectId id = ProjectId.parse(projectId);
    Project project = loadProject(id);
    Experiment activeExperiment = loadActiveExperimentForProject(project);
    activeExperiment.addAnalytes(List.of(analytes));
    experimentRepository.update(activeExperiment);
  }

  @PostAuthorize("hasPermission(returnObject,'VIEW_PROJECT')")
  public Optional<Project> find(ProjectId projectId) {
    log.debug("Search for project with id: " + projectId.toString());
    return projectRepository.find(projectId);
  }

  public void updateTitle(String projectId, String newTitle) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectTitle projectTitle = ProjectTitle.of(newTitle);
    Project project = loadProject(projectIdentifier);
    project.updateTitle(projectTitle);
    projectRepository.update(project);
  }

  public void manageProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Project project = loadProject(projectIdentifier);
    project.setProjectManager(personReference);
    projectRepository.update(project);
  }

  public void investigateProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Project project = loadProject(projectIdentifier);
    project.setPrincipalInvestigator(personReference);
    projectRepository.update(project);
  }

  public void setResponsibility(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Project project = loadProject(projectIdentifier);
    project.setResponsiblePerson(personReference);
    projectRepository.update(project);
  }

  public void describeExperimentalDesign(String projectId, String experimentalDesign) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ExperimentalDesignDescription experimentalDesignDescription = ExperimentalDesignDescription.create(
        experimentalDesign);
    Project project = loadProject(projectIdentifier);
    project.describeExperimentalDesign(experimentalDesignDescription);
    projectRepository.update(project);
  }

  public void stateObjective(String projectId, String objective) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Project project = loadProject(projectIdentifier);
    project.stateObjective(projectObjective);
    projectRepository.update(project);
  }


  /**
   * Retrieve all analytes of the active experiment.
   *
   * @param projectId the project the experiment belongs to
   * @return a collection of analytes in the active experiment.
   */
  public Collection<Analyte> getAnalytesOfActiveExperiment(ProjectId projectId) {
    Project project = loadProject(projectId);
    Experiment experiment = loadActiveExperimentForProject(project);
    return experiment.getAnalytes();
  }


  /**
   * Retrieve all species of the active experiment.
   *
   * @param projectId the project the experiment belongs to
   * @return a collection of species in the active experiment.
   */
  public Collection<Species> getSpeciesOfActiveExperiment(ProjectId projectId) {
    Project project = loadProject(projectId);
    Experiment experiment = loadActiveExperimentForProject(project);
    return experiment.getSpecies();
  }

  /**
   * Retrieve all specimen of the active experiment.
   *
   * @param projectId the project the experiment belongs to
   * @return a collection of specimen in the active experiment.
   */
  public Collection<Specimen> getSpecimensOfActiveExperiment(ProjectId projectId) {
    Project project = loadProject(projectId);
    Experiment experiment = loadActiveExperimentForProject(project);
    return experiment.getSpecimens();
  }

  public Collection<Experiment> getExperimentsForProject(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    Project project = loadProject(projectId);
    return project.experiments().stream().map(
            experimentId -> experimentRepository.find(experimentId).orElseThrow(
                () -> new ProjectManagementException("Failed to find experiment " + experimentId)))
        .toList();
  }
}
