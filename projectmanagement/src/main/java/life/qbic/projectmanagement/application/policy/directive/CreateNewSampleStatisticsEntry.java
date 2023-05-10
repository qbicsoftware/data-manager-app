package life.qbic.projectmanagement.application.policy.directive;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.event.ProjectRegisteredEvent;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class CreateNewSampleStatisticsEntry implements
    DomainEventSubscriber<ProjectRegisteredEvent> {

  private static final Logger log = logger(CreateNewSampleStatisticsEntry.class);

  private final SampleCodeService sampleCodeService;

  private final ProjectRepository projectRepository;
  private final JobScheduler jobScheduler;

  public CreateNewSampleStatisticsEntry(SampleCodeService sampleCodeService,
      JobScheduler jobScheduler, ProjectRepository projectRepository) {
    this.sampleCodeService = requireNonNull(sampleCodeService);
    this.jobScheduler = requireNonNull(jobScheduler);
    this.projectRepository = requireNonNull(projectRepository);
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ProjectRegisteredEvent.class;
  }

  @Override
  public void handleEvent(ProjectRegisteredEvent event) {
    jobScheduler.enqueue(() -> createSampleStatisticsEntry(event.createdProject()));
  }

  public void createSampleStatisticsEntry(String projectId) {
    Optional<Project> searchResult = projectRepository.find(ProjectId.parse(projectId)).stream()
        .findFirst();
    if (searchResult.isEmpty()) {
      throw new RuntimeException("Domain event processing failed!");
    }
    sampleCodeService.addProjectToSampleStats(searchResult.get().getId(),
        searchResult.get().getProjectCode());
  }
}
