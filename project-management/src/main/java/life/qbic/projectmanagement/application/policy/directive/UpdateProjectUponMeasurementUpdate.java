package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository.ProjectNotFoundException;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Update project modified timestamp upon update of a measurement in that project</b>
 * <p>
 * After a measurement has been updated, we need to update the timestamp of the lastModified
 * property of the respective project
 *
 * @since 1.0.0
 */
@Component
public class UpdateProjectUponMeasurementUpdate implements DomainEventSubscriber<MeasurementUpdatedEvent> {

  private final ProjectInformationService projectInformationService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponMeasurementUpdate(ProjectInformationService projectInformationService,
      JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return MeasurementUpdatedEvent.class;
  }

  @Override
  public void handleEvent(MeasurementUpdatedEvent event) {
    jobScheduler.enqueue(
        () -> updateProjectModified(event.projectId(), event.measurementId(), event.occurredOn()));
  }

  @Job(name = "update project upon measurement update of measurement %0")
  public void updateProjectModified(String projectId, String measurementID,
      Instant modifiedOn) throws ProjectNotFoundException {
    if (Optional.ofNullable(measurementID).isEmpty()) {
      return;
    }
    projectInformationService.updateModifiedDate(ProjectId.parse(projectId), modifiedOn);

  }
}
