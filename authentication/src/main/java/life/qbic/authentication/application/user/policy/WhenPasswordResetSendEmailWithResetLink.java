package life.qbic.authentication.application.user.policy;

import life.qbic.authentication.application.communication.UserContactService;
import life.qbic.authentication.domain.user.event.PasswordResetRequested;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * When a password reset request reset event occurred, email the user with a link to reset their
 * password.
 *
 * @since 1.0.0
 */
@Component
public class WhenPasswordResetSendEmailWithResetLink implements
    DomainEventSubscriber<PasswordResetRequested> {

  private final UserContactService userContactService;

  private final JobScheduler jobScheduler;

  public WhenPasswordResetSendEmailWithResetLink(@Autowired UserContactService userContactService,
      @Autowired JobScheduler jobScheduler) {
    this.userContactService = userContactService;
    this.jobScheduler = jobScheduler;
    DomainEventDispatcher.instance().subscribe(this);
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return PasswordResetRequested.class;
  }

  @Override
  public void handleEvent(PasswordResetRequested event) {
    jobScheduler.enqueue(
        () -> userContactService.sendResetLink(event.userId().get()));
  }
}
