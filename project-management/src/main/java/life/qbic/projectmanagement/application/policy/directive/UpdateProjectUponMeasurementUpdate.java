package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;
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
  private final MeasurementLookupService measurementLookupService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponMeasurementUpdate(MeasurementLookupService measurementLookupService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.measurementLookupService = measurementLookupService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return MeasurementUpdatedEvent.class;
  }

  @Override
  public void handleEvent(MeasurementUpdatedEvent event) {
    jobScheduler.enqueue(() -> updateProjectModified(event.measurementId(), event.occurredOn()));
  }

  @Job(name = "update project upon measurement update of measurement %0")
  public void updateProjectModified(MeasurementId measurementID, Instant modifiedOn) throws ProjectNotFoundException {
    Optional<NGSMeasurement> ngs = measurementLookupService.findNGSMeasurementById(measurementID.value());
    if(ngs.isPresent()) {
      projectInformationService.updateModifiedDate(ngs.get().projectId(), modifiedOn);
    } else {
      Optional<ProteomicsMeasurement> ptx = measurementLookupService.findProteomicsMeasurementById(
          measurementID.value());
      if(ptx.isEmpty()) {
        throw new InvalidEventDataException("Measurement not found.");
      }
      projectInformationService.updateModifiedDate(ptx.get().projectId(), modifiedOn);
    }
  }
}
