package life.qbic.projectmanagement.application;

import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectIntent;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
import life.qbic.projectmanagement.domain.service.ProjectDomainService;
import org.springframework.stereotype.Service;

/**
 * Application service facilitating the creation of projects.
 */
@Service
public class ProjectCreationService {

  private final ProjectDomainService projectDomainService;

  public ProjectCreationService(ProjectDomainService projectDomainService) {
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
      Contact projectManager,
      Funding funding) {
    if (Objects.isNull(principalInvestigator)) {
      return Result.fromError(new ApplicationException("principal investigator is null"));
    }
    if (Objects.isNull(projectManager)) {
      return Result.fromError(new ApplicationException("project manager is null"));
    }
    try {
      Project project = createProject(code, title, objective, projectManager,
          principalInvestigator, responsiblePerson, funding);
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
      Contact principalInvestigator, Contact responsiblePerson, Funding funding) {
    ProjectIntent intent = getProjectIntent(title, objective);
    ProjectCode projectCode;
    try {
      projectCode = ProjectCode.parse(code);
    } catch (IllegalArgumentException exception) {
      throw new ApplicationException("Project code: " + code + " is invalid.", exception,
          ErrorCode.INVALID_PROJECT_CODE,
          ErrorParameters.of(code, ProjectCode.getPREFIX(), ProjectCode.getLENGTH()));
    }
    return projectDomainService.registerProject(intent, projectCode,
        projectManager, principalInvestigator, responsiblePerson, funding);
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
