package life.qbic.projectmanagement.domain.project.service;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectIntent;
import life.qbic.projectmanagement.domain.project.event.ProjectRegisteredEvent;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Project Domain Service</b>
 * <p>
 * Responsible for the {@link Project} entity creation, will dispatch a
 * {@link ProjectRegisteredEvent}, when a new project was registered successfully.
 *
 * @since 1.0.0
 */
@Service
public class ProjectDomainService {

  private static final Logger log = logger(ProjectDomainService.class);
  private final ProjectRepository projectRepository;

  @Autowired
  public ProjectDomainService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  /**
   * Registers a new project entity.
   * <p>
   * Note: Dispatches a {@link ProjectRegisteredEvent} after a successful registration
   *
   * @param projectIntent         the project intend
   * @param projectCode           the assigned project code
   * @param projectManager        the responsible project manager
   * @param principalInvestigator the principal investigator
   * @param responsiblePerson     the responsible person on the customer side for the project.
   * @return a {@link Result} of the registration
   * @since 1.0.0
   */
  public Result<Project, ResponseCode> registerProject(
      ProjectIntent projectIntent, ProjectCode projectCode,
      PersonReference projectManager, PersonReference principalInvestigator,
      PersonReference responsiblePerson) {
    var project = Project.create(projectIntent, projectCode,
        projectManager, principalInvestigator,
        responsiblePerson);

    try {
      projectRepository.add(project);
    } catch (Exception e) {
      log.error("Project with code " + project.getProjectCode() + " registration failed.", e);
      return Result.fromError(ResponseCode.PROJECT_REGISTRATION_FAILED);
    }

    // In case of a successful registration, we dispatch the registration event
    DomainEventDispatcher.instance().dispatch(ProjectRegisteredEvent.create(project.getId()));

    return Result.fromValue(project);
  }

  public enum ResponseCode {
    PROJECT_REGISTRATION_FAILED
  }

}
