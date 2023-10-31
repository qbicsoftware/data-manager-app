package life.qbic.identity.application.user.policy.directive;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.broadcasting.MessageParameters;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSerializer;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.communication.broadcasting.IntegrationEvent;
import life.qbic.identity.domain.event.UserRegistered;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * When a user registered, broadcast an integration event to message bus
 *
 * @since 1.0.0
 */
@Component
public class WhenUserRegisteredSubmitIntegrationEvent implements
    DomainEventSubscriber<UserRegistered> {

  private final MessageBusSubmission messageBusSubmission;

  private final EventHub eventHub;

  private final JobScheduler jobScheduler;

  public WhenUserRegisteredSubmitIntegrationEvent(
      @Autowired MessageBusSubmission messageBusSubmission,
      @Autowired EventHub eventHub,
      @Autowired JobScheduler jobScheduler) {
    this.messageBusSubmission = messageBusSubmission;
    this.jobScheduler = jobScheduler;
    this.eventHub = eventHub;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return UserRegistered.class;
  }

  @Override
  public void handleEvent(UserRegistered event) {
    this.jobScheduler.enqueue(() -> notifyMessageBus(event));
    dispatchEvent(event);
  }

  @Job(name = "Notify message bus about user registration")
  public void notifyMessageBus(UserRegistered event) {
    messageBusSubmission.submit(new DomainEventSerializer().serialize(event),
        MessageParameters.durableTextParameters("UserRegistered", UUID.randomUUID().toString(),
            Instant.now()));
  }

  public void dispatchEvent(UserRegistered event) {
    Map<String, String> content = new HashMap<>();
    content.put("userId", event.userId());
    IntegrationEvent integrationEvent = IntegrationEvent.create("userRegistered", content);
    eventHub.send(integrationEvent);
  }
}
