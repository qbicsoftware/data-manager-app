package life.qbic.identity.application.user.policy;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.broadcasting.MessageParameters;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSerializer;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
import life.qbic.identity.application.communication.Messages;
import life.qbic.identity.domain.event.UserRegistered;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
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

  private final JobScheduler jobScheduler;

  public WhenUserRegisteredSubmitIntegrationEvent(
      @Autowired MessageBusSubmission messageBusSubmission,
      @Autowired JobScheduler jobScheduler) {
    this.messageBusSubmission = messageBusSubmission;
    DomainEventDispatcher.instance().subscribe(this);
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return UserRegistered.class;
  }

  @Override
  public void handleEvent(UserRegistered event) {
    this.jobScheduler.enqueue(() -> notifyMessageBus(event));
  }

  @Job(name = "Notify message bus about user registration")
  public void notifyMessageBus(UserRegistered event) {
    messageBusSubmission.submit(new DomainEventSerializer().serialize(event),
        MessageParameters.durableTextParameters("UserRegistered", UUID.randomUUID().toString(),
            Instant.now()));
  }
}
