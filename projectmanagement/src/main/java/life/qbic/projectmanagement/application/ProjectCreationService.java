package life.qbic.projectmanagement.application;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectIntent;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;

/**
 * Application service facilitating the creation of projects.
 */
public class ProjectCreationService {

  private static final Logger log = logger(ProjectCreationService.class);

  private final ProjectRepository projectRepository;

  public ProjectCreationService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  /**
   * Create a new project based on the information provided. If an empty experimental design
   * description is provided, the project will not have an experimental design described.
   *
   * @param title              the title of the project.
   * @param objective          the objective of the project
   * @param experimentalDesign a description of the experimental design
   * @return the created project
   */
  public Result<Project, ApplicationException> createProject(String title, String objective,
      String experimentalDesign, String sourceOffer, PersonReference projectManager,
      PersonReference principalInvestigator) {
    try {
      Project project;
      if (Objects.isNull(experimentalDesign) || experimentalDesign.isEmpty()) {
        project = createProjectWithoutExperimentalDesign(title, objective, projectManager,
            principalInvestigator);
      } else {
        project = createProjectWithExperimentalDesign(title, objective, experimentalDesign,
            projectManager, principalInvestigator);
      }
      Optional.ofNullable(sourceOffer)
          .flatMap(it -> it.isBlank() ? Optional.empty() : Optional.of(it))
          .ifPresent(offerIdentifier -> project.linkOffer(OfferIdentifier.of(offerIdentifier)));
      projectRepository.add(project);
      return Result.success(project);
    } catch (ProjectManagementException projectManagementException) {
      return Result.failure(projectManagementException);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException());
    }
  }

  private ProjectCode createRandomCode() {
    ProjectCode code = ProjectCode.random();
    while (!projectRepository.find(code).isEmpty()) {
      log.warn(String.format("Random generated code exists: %s", code.value()));
      code = ProjectCode.random();
    }
    log.info(String.format("Created new random project code '%s'", code.value()));
    return code;
  }

  private Project createProjectWithoutExperimentalDesign(String title, String objective,
      PersonReference projectManager,
      PersonReference principalInvestigator) {
    ProjectIntent intent = getProjectIntent(title, objective);
    return Project.create(intent, createRandomCode(), projectManager, principalInvestigator);
  }

  private Project createProjectWithExperimentalDesign(String title,
      String objective,
      String experimentalDesign, PersonReference projectManager,
      PersonReference principalInvestigator) {

    ExperimentalDesignDescription experimentalDesignDescription;
    try {
      experimentalDesignDescription = ExperimentalDesignDescription.create(experimentalDesign);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      throw new ProjectManagementException(ErrorCode.INVALID_EXPERIMENTAL_DESIGN,
          ErrorParameters.of(ExperimentalDesignDescription.maxLength(), experimentalDesign));
    }

    ProjectIntent intent = getProjectIntent(title, objective).with(experimentalDesignDescription);
    return Project.create(intent, createRandomCode(), projectManager, principalInvestigator);
  }

  private static ProjectIntent getProjectIntent(String title, String objective) {
    ProjectTitle projectTitle;
    try {
      projectTitle = ProjectTitle.create(title);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      throw new ProjectManagementException(ErrorCode.INVALID_PROJECT_TITLE,
          ErrorParameters.of(ProjectTitle.maxLength(), title));
    }

    ProjectObjective projectObjective;
    try {
      projectObjective = ProjectObjective.create(objective);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      throw new ProjectManagementException(ErrorCode.INVALID_PROJECT_OBJECTIVE,
          ErrorParameters.of(objective));
    }

    return ProjectIntent.of(projectTitle, projectObjective);
  }
}
