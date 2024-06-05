package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.event.ExperimentUpdatedEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository.ProjectNotFoundException;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Update project modified timestamp upon update of an experiment in that project</b>
 * <p>
 * After an experiment has been updated, we need to update the timestamp of the lastModified
 * property of the respective project
 *
 * @since 1.0.0
 */
@Component
public class UpdateProjectUponExperimentUpdate implements DomainEventSubscriber<ExperimentUpdatedEvent> {

  private final ProjectInformationService projectInformationService;
  private final ExperimentInformationService experimentInformationService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponExperimentUpdate(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService, JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ExperimentUpdatedEvent.class;
  }

  @Override
  public void handleEvent(ExperimentUpdatedEvent event) {
    jobScheduler.enqueue(() -> updateProjectModified(event.experimentId(), event.occurredOn()));
  }

  @Job(name = "Update project upon update of experiment %0")
  public void updateProjectModified(ExperimentId experimentId, Instant modifiedOn) throws ProjectNotFoundException {
    Optional<ProjectId> projectId = experimentInformationService.findProjectID(experimentId);
    if(projectId.isEmpty()) {
      throw new InvalidEventDataException("Project Id not found.");
    }
    projectInformationService.updateModifiedDate(projectId.get(), modifiedOn);
  }
}
