package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlService;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControl;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControlCreatedEvent;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Update project modified timestamp upon creation of a qc object in that project</b>
 * <p>
 * After qc object has been added, we need to update the timestamp of the lastModified property of
 * the respective project
 *
 * @since 1.0.0
 */
@Component
public class UpdateProjectUponQCCreation implements
    DomainEventSubscriber<QualityControlCreatedEvent> {

  private final ProjectInformationService projectInformationService;
  private final QualityControlService qualityControlService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponQCCreation(QualityControlService qualityControlService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.qualityControlService = qualityControlService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return QualityControlCreatedEvent.class;
  }

  @Override
  public void handleEvent(QualityControlCreatedEvent event) {
    jobScheduler.enqueue(() -> updateProjectModified(event.qualityControlID(), event.occurredOn()));
  }

  @Job(name = "Update project upon QC item creation of item %0")
  public void updateProjectModified(Long qcID, Instant modifiedOn) {
    Optional<QualityControl> qc = qualityControlService.getQualityControl(qcID);
    if (qc.isEmpty()) {
      throw new InvalidEventDataException("QC item not found.");
    }
    projectInformationService.updateModifiedDate(qc.get().project(), modifiedOn);
  }
}
