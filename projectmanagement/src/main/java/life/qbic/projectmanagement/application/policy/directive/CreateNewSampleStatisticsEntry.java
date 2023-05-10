package life.qbic.projectmanagement.application.policy.directive;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.event.ProjectRegisteredEvent;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class CreateNewSampleStatisticsEntry implements
    DomainEventSubscriber<ProjectRegisteredEvent> {

  private static final Logger log = logger(CreateNewSampleStatisticsEntry.class);

  private final SampleCodeService sampleCodeService;

  private final ProjectInformationService projectInformationService;
  private final JobScheduler jobScheduler;

  public CreateNewSampleStatisticsEntry(SampleCodeService sampleCodeService,
      JobScheduler jobScheduler, ProjectInformationService projectInformationService) {
    this.sampleCodeService = requireNonNull(sampleCodeService);
    this.jobScheduler = requireNonNull(jobScheduler);
    this.projectInformationService = requireNonNull(projectInformationService);
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ProjectRegisteredEvent.class;
  }

  @Override
  public void handleEvent(ProjectRegisteredEvent event) {
    jobScheduler.enqueue(() -> createSampleStatisticsEntry(event.createdProject()));
  }

  private void createSampleStatisticsEntry(String projectId) {
    Optional<Project> searchResult = projectInformationService.find(projectId);
    if (searchResult.isEmpty()) {
      throw new RuntimeException("Domain event processing failed!");
    }
    sampleCodeService.addProjectToSampleStats(searchResult.get().getId(),
        searchResult.get().getProjectCode());
  }
}
