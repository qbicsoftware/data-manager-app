package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.event.BatchRegistered;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Update project modified timestamp upon creation of a batch in that project</b>
 * <p>
 * After a batch has been created, we need to update the timestamp of the lastModified
 * property of the respective project
 *
 * @since 1.0.0
 */
@Component
public class UpdateProjectUponBatchCreation implements DomainEventSubscriber<BatchRegistered> {

  private final ProjectInformationService projectInformationService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponBatchCreation(ProjectInformationService projectInformationService,
      JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return BatchRegistered.class;
  }

  @Override
  public void handleEvent(BatchRegistered event) {
    jobScheduler.enqueue(() -> updateProjectModified(event.projectId(), event.batchId(),
        event.occurredOn()));
  }

  @Job(name = "Update project %0 upon batch creation of batch %1")
  public void updateProjectModified(ProjectId projectId, BatchId batchId, Instant modifiedOn) {
    projectInformationService.updateModifiedDate(projectId, modifiedOn);
  }
}
