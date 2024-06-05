package life.qbic.projectmanagement.application.policy.directive;

import java.time.Instant;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.purchase.PurchaseCreatedEvent;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Update project modified timestamp upon change creation of a purchase object</b>
 * <p>
 * After a purchase object has been added, we need to update the timestamp of the lastModified
 * property of the respective project
 *
 * @since 1.0.0
 */
@Component
public class UpdateProjectUponPurchaseCreation implements
    DomainEventSubscriber<PurchaseCreatedEvent> {

  private final ProjectInformationService projectInformationService;
  private final ProjectPurchaseService projectPurchaseService;
  private final JobScheduler jobScheduler;

  public UpdateProjectUponPurchaseCreation(ProjectPurchaseService projectPurchaseService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    this.projectInformationService = projectInformationService;
    this.projectPurchaseService = projectPurchaseService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return PurchaseCreatedEvent.class;
  }

  @Override
  public void handleEvent(PurchaseCreatedEvent event) {
    jobScheduler.enqueue(() -> updateProjectModified(event.purchaseID(), event.occurredOn()));
  }

  @Job(name = "Update project upon creation of purchase item %0")
  public void updateProjectModified(Long purchaseID, Instant modifiedOn) {
    Optional<ProjectId> projectId = projectPurchaseService.findProjectIdOfPurchase(purchaseID);
    if(projectId.isEmpty()) {
      throw new InvalidEventDataException("Purchase item not found.");
    }
    projectInformationService.updateModifiedDate(projectId.get(), modifiedOn);
  }
}
