package life.qbic.projectmanagement.application;

import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.*;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static life.qbic.logging.service.LoggerFactory.logger;

/**
 * Application service facilitating the creation of projects.
 */
@Service
public class ProjectCreationService {

  private static final Logger log = logger(ProjectCreationService.class);

  private final ProjectRepository projectRepository;

  public ProjectCreationService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  /**
   * Create a new project based on the information provided.
   *
   * @param title              the title of the project.
   * @param objective          the objective of the project
   * @param experimentalDesign a description of the experimental design
   * @return the created project
   */
  public Result<Project, ApplicationException> createProject(String code, String title, String objective,
      String experimentalDesign, String sourceOffer, PersonReference projectManager,
      PersonReference principalInvestigator, PersonReference responsiblePerson) {
    try {
      Project project;
      project = createProject(code, title, objective, experimentalDesign,
          projectManager, principalInvestigator, responsiblePerson);
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

  private Project createProject(String code, String title,
      String objective,
      String experimentalDesign, PersonReference projectManager,
      PersonReference principalInvestigator, PersonReference responsiblePerson) {

    ExperimentalDesignDescription experimentalDesignDescription;
    try {
      experimentalDesignDescription = ExperimentalDesignDescription.create(experimentalDesign);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      throw new ProjectManagementException(ErrorCode.INVALID_EXPERIMENTAL_DESIGN,
          ErrorParameters.of(ExperimentalDesignDescription.maxLength(), experimentalDesign));
    }

    ProjectIntent intent = getProjectIntent(title, objective).with(experimentalDesignDescription);
    ProjectCode projectCode;
    try {
      projectCode = ProjectCode.parse(code);
      if(!projectRepository.find(projectCode).isEmpty()) {
        log.info("Project code: "+code+ " is already in use.");
        throw new ProjectManagementException("Project code "+code+" is already in use.");
      }
    } catch (IllegalArgumentException exception) {
      log.info("Project code: "+code+ " is invalid.");
      log.info(exception.getMessage());
      throw new ProjectManagementException("Project code "+code+" is invalid.");
    }
    return Project.create(intent, projectCode, projectManager, principalInvestigator,
        responsiblePerson);
  }

  private static ProjectIntent getProjectIntent(String title, String objective) {
    ProjectTitle projectTitle;
    try {
      projectTitle = ProjectTitle.of(title);
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
