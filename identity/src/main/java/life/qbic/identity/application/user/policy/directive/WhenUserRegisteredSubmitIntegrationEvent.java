package life.qbic.identity.application.user.policy.directive;

import java.util.HashMap;
import java.util.Map;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.communication.broadcasting.IntegrationEvent;
import life.qbic.identity.domain.event.UserRegistered;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When a user registered, broadcast an integration event to message bus
 *
 * @since 1.0.0
 */
public class WhenUserRegisteredSubmitIntegrationEvent implements
    DomainEventSubscriber<UserRegistered> {

  private final EventHub eventHub;

  private final JobScheduler jobScheduler;

  public WhenUserRegisteredSubmitIntegrationEvent(
      @Autowired EventHub eventHub,
      @Autowired JobScheduler jobScheduler) {
    this.jobScheduler = jobScheduler;
    this.eventHub = eventHub;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return UserRegistered.class;
  }

  @Override
  public void handleEvent(UserRegistered event) {
    this.jobScheduler.enqueue(() -> dispatchEvent(event));
  }

  @Job(name = "Broadcast user registration")
  public void dispatchEvent(UserRegistered event) {
    Map<String, String> content = new HashMap<>();
    content.put("userId", event.userId());
    IntegrationEvent integrationEvent = IntegrationEvent.create("userRegistered", content);
    eventHub.send(integrationEvent);
  }
}
