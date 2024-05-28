package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;
import life.qbic.projectmanagement.domain.repository.ProjectRepository.ProjectNotFoundException;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Update project modified timestamp upon creation of a sample in that project</b>
 * <p>
 * After a sample has been created, we need to update the timestamp of the lastModified
 * property of the respective project
 *
 * @since 1.0.0
 */
@Component
public class UpdateProjectUponSampleCreation implements DomainEventSubscriber<SampleRegistered> {

  private final ProjectInformationService projectInformationService;
  private final ExperimentInformationService experimentInformationService;
  private final SampleInformationService sampleInformationService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponSampleCreation(SampleInformationService sampleInformationService,
      ExperimentInformationService experimentInformationService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.sampleInformationService = sampleInformationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return SampleRegistered.class;
  }

  @Override
  public void handleEvent(SampleRegistered event) {
    jobScheduler.enqueue(() -> updateProjectModified(event.registeredSample(), event.occurredOn()));
  }

  @Job(name = "Update_Project_Modified")
  public void updateProjectModified(SampleId sampleID, Instant modifiedOn) throws ProjectNotFoundException {
    Optional<Sample> sample = sampleInformationService.findSample(sampleID);
    if(sample.isPresent()) {
      experimentInformationService.find(sample.get().experimentId())
          .ifPresent(experiment -> projectInformationService.updateModifiedDate(
          experiment..projectId(), modifiedOn));
    }
    Optional<NGSMeasurement> ngs = measurementLookupService.findNGSMeasurement(measurementID.value());
    if(ngs.isPresent()) {
      projectInformationService.updateModifiedDate(ngs.get().projectId(), modifiedOn);
    } else {
      Optional<ProteomicsMeasurement> ptx = measurementLookupService.findProteomicsMeasurement(
          measurementID.value());
      ptx.ifPresent(
    }

  }
}
