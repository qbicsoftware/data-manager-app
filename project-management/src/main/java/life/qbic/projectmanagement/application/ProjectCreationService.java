package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectContact;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectContacts;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectIntent;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
import life.qbic.projectmanagement.domain.service.ProjectDomainService;
import org.springframework.lang.Nullable;
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
   * @param code        the project's code
   * @param title       the project title
   * @param objective   the project objective
   * @since 1.10.0
   */
  public Result<Project, ApplicationException> createProject(
      @Nullable String sourceOffer,
      String code,
      String title,
      String objective,
      ProjectContacts contacts,
      @Nullable FundingInformation funding) {
    requireNonNull(code);
    requireNonNull(title);
    requireNonNull(objective);
    requireNonNull(contacts);

    Funding fundingInformation = Optional.ofNullable(funding)
        .map(this::convertFundingInformation)
        .orElse(null);
    Contact responsiblePerson = Optional.ofNullable(contacts.responsible())
        .map(this::convertProjectContact)
        .orElse(null);

    try {
      var createdProject = createProject(code, title, objective,
          convertProjectContact(contacts.investigator()),
          convertProjectContact(contacts.manager()),
          responsiblePerson,
          fundingInformation);
      Optional.ofNullable(sourceOffer)
          .filter(it -> !it.isBlank())
          .ifPresent(offerId -> createdProject.linkOffer(
          OfferIdentifier.of(offerId)));
      return Result.fromValue(createdProject);
    } catch (ApplicationException e) {
      return Result.fromError(e);
    }

  }

  private Contact convertProjectContact(ProjectContact contact) {
    return new Contact(contact.fullName(), contact.email(), contact.oidc(), contact.oidcIssuer());
  }

  private Funding convertFundingInformation(FundingInformation funding) {
    return Funding.of(funding.grantId(), funding.grant());
  }

  private Project createProject(String code, String title, String objective,
      Contact projectManager,
      Contact principalInvestigator, @Nullable Contact responsiblePerson,
      @Nullable Funding funding) {

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
