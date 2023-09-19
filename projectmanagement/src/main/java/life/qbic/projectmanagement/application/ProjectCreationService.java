package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.Contact;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectIntent;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.project.service.ProjectDomainService;
import life.qbic.projectmanagement.domain.project.service.ProjectDomainService.ResponseCode;
import org.springframework.stereotype.Service;

/**
 * Application service facilitating the creation of projects.
 */
@Service
public class ProjectCreationService {

  private static final Logger log = logger(ProjectCreationService.class);

  private final ProjectRepository projectRepository;
  private final ProjectDomainService projectDomainService;

  public ProjectCreationService(ProjectRepository projectRepository,
      ProjectDomainService projectDomainService) {
    this.projectRepository = requireNonNull(projectRepository);
    this.projectDomainService = requireNonNull(projectDomainService);
  }


  /**
   * Create a new project based on the information provided
   *
   * @param sourceOffer the offer from which information was taken
   * @param code        the projects code
   * @param title       the project title
   * @param objective   the project objective
   * @return a result containing the project or an exception
   */
  public Result<Project, ApplicationException> createProject(String sourceOffer,
      String code,
      String title,
      String objective,
      Contact principalInvestigator,
      Contact responsiblePerson,
      Contact projectManager) {
    if (Objects.isNull(principalInvestigator)) {
      return Result.fromError(new ApplicationException("principal investigator is null"));
    }
    if (Objects.isNull(projectManager)) {
      return Result.fromError(new ApplicationException("project manager is null"));
    }

    try {
      Project project = createProject(code, title, objective, projectManager,
          principalInvestigator, responsiblePerson);
      Optional.ofNullable(sourceOffer)
          .flatMap(it -> it.isBlank() ? Optional.empty() : Optional.of(it))
          .ifPresent(offerIdentifier -> project.linkOffer(OfferIdentifier.of(offerIdentifier)));
      return Result.fromValue(project);
    } catch (RuntimeException e) {
      return Result.fromError(ApplicationException.wrapping(e));
    }
  }

  private Project createProject(String code, String title, String objective,
      Contact projectManager,
      Contact principalInvestigator, Contact responsiblePerson) {
    ProjectIntent intent = getProjectIntent(title, objective);
    ProjectCode projectCode;
    try {
      projectCode = ProjectCode.parse(code);
      if (!projectRepository.find(projectCode).isEmpty()) {
        throw new ApplicationException("Project code: " + code + " is already in use.",
            ErrorCode.DUPLICATE_PROJECT_CODE,
            ErrorParameters.of(code));
      }
    } catch (IllegalArgumentException exception) {
      throw new ApplicationException("Project code: " + code + " is invalid.", exception,
          ErrorCode.INVALID_PROJECT_CODE,
          ErrorParameters.of(code, ProjectCode.getPREFIX(), ProjectCode.getLENGTH()));
    }

    var registrationResult = projectDomainService.registerProject(intent, projectCode, projectManager, principalInvestigator, responsiblePerson);

    if (registrationResult.isError() && registrationResult.getError().equals(ResponseCode.PROJECT_REGISTRATION_FAILED)) {
      throw new ApplicationException("Project registration failed.", ErrorCode.GENERAL, ErrorParameters.of(code));
    }

    return registrationResult.getValue();
  }

  private static ProjectIntent getProjectIntent(String title, String objective) {
    ProjectTitle projectTitle;
    try {
      projectTitle = ProjectTitle.of(title);
    } catch (RuntimeException e) {
      throw new ApplicationException(
          "could not get project intent from title " + title, e,
          ErrorCode.INVALID_PROJECT_TITLE,
          ErrorParameters.of(ProjectTitle.maxLength(), title));
    }

    ProjectObjective projectObjective;
    try {
      projectObjective = ProjectObjective.create(objective);
    } catch (RuntimeException e) {
      throw new ApplicationException(
          "could not get project intent from objective " + objective, e,
          ErrorCode.INVALID_PROJECT_OBJECTIVE,
          ErrorParameters.of(objective));
    }

    return ProjectIntent.of(projectTitle, projectObjective);
  }
}
