package life.qbic.identity.application.user.policy.directive;

import java.util.HashMap;
import java.util.Map;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.communication.broadcasting.IntegrationEvent;
import life.qbic.identity.domain.event.UserActivated;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * Subscribes to user activation. When a user was activated, publishes a corresponding integration
 * event.
 */
@Component
public class WhenUserActivatedSubmitIntegrationEvent implements
    DomainEventSubscriber<UserActivated> {

  private final JobScheduler jobScheduler;
  private final EventHub eventHub;

  public WhenUserActivatedSubmitIntegrationEvent(EventHub eventHub,
      JobScheduler jobScheduler) {
    this.eventHub = eventHub;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return UserActivated.class;
  }

  @Override
  public void handleEvent(UserActivated event) {
    this.jobScheduler.enqueue(() -> dispatchEvent(event));
  }

  @Job(name = "Broadcast user activation")
  public void dispatchEvent(UserActivated event) {
    Map<String, String> content = new HashMap<>();
    content.put("userId", event.userId());
    IntegrationEvent integrationEvent = IntegrationEvent.create("userActivated", content);
    eventHub.send(integrationEvent);
  }
}
