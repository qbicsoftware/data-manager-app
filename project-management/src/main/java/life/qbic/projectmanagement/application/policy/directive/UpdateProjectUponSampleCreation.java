package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;
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

  @Job(name = "Update project upon sample creation of sample %0")
  public void updateProjectModified(SampleId sampleID, Instant modifiedOn) {
    Optional<Sample> sample = sampleInformationService.findSample(sampleID);
    if (sample.isEmpty()) {
      throw new InvalidEventDataException("Sample not found.");
    }
    ProjectId projectId = experimentInformationService.findProjectID(sample.get().experimentId())
        .orElseThrow(() -> new InvalidEventDataException("Project Id not found"));
    projectInformationService.updateModifiedDate(projectId, modifiedOn);
  }
}
