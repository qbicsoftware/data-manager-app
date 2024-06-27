package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged;
import life.qbic.projectmanagement.domain.repository.ProjectRepository.ProjectNotFoundException;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Update project modified timestamp</b>
 * <p>
 * After a domain object in a project has been deleted, we need to update the timestamp of the
 * lastModified property of the project. Since the id of the deleted object cannot be found any more,
 * this is directive listens to the project changed event that is already aware of the project id
 *
 * @since 1.0.0
 */
@Component
public class UpdateProjectUponDeletionEvent implements DomainEventSubscriber<ProjectChanged> {

  private final ProjectInformationService projectInformationService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponDeletionEvent(ProjectInformationService projectInformationService,
      JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ProjectChanged.class;
  }

  @Override
  public void handleEvent(ProjectChanged event) {
    jobScheduler.enqueue(() -> updateProjectModified(event.projectId(), event.occurredOn()));
  }

  @Job(name = "Update project modification upon deletion event for project %0")
  public void updateProjectModified(ProjectId projectID, Instant modifiedOn) throws ProjectNotFoundException {
    projectInformationService.updateModifiedDate(projectID, modifiedOn);
  }
}
