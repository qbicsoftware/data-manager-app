package life.qbic.projectmanagement.domain.service;

import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectIntent;
import life.qbic.projectmanagement.domain.model.project.event.ProjectRegisteredEvent;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.repository.ProjectRepository.ProjectExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
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
  public Project registerProject(
      ProjectIntent projectIntent, ProjectCode projectCode,
      Contact projectManager, Contact principalInvestigator,
      @Nullable Contact responsiblePerson, @Nullable Funding funding) {
    var project = Project.create(projectIntent, projectCode,
        projectManager, principalInvestigator,
        responsiblePerson);
    Optional.ofNullable(funding).ifPresent(project::setFunding);
    try {
      projectRepository.add(project);
    } catch (ProjectExistsException projectExistsException) {
      throw new ApplicationException("Project code is " + projectCode + " already in use.",
          projectExistsException,
          ErrorCode.DUPLICATE_PROJECT_CODE, ErrorParameters.of(projectCode.value()));
    } catch (RuntimeException exception) {
      throw new ApplicationException("Registration of project with code "
          + projectCode + " failed.", exception);
    }
    // In case of a successful registration, we dispatch the registration event
    DomainEventDispatcher.instance().dispatch(ProjectRegisteredEvent.create(project.getId()));
    return project;
  }
}
