package life.qbic.identity.application.user.policy;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Messages;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.identity.domain.event.PasswordResetRequested;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserRepository;
import org.jobrunr.jobs.annotations.Job;
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
public class WhenPasswordResetNotifyUser implements
    DomainEventSubscriber<PasswordResetRequested> {

  private final EmailService emailService;

  private final JobScheduler jobScheduler;

  private final UserRepository userRepository;
  private final PasswordResetLinkSupplier passwordResetLinkSupplier;

  public WhenPasswordResetNotifyUser(@Autowired EmailService emailService,
      @Autowired UserRepository userRepository,
      @Autowired JobScheduler jobScheduler,
      @Autowired PasswordResetLinkSupplier passwordResetLinkSupplier) {
    this.emailService = emailService;
    this.jobScheduler = jobScheduler;
    this.userRepository = userRepository;
    this.passwordResetLinkSupplier = passwordResetLinkSupplier;
    DomainEventDispatcher.instance().subscribe(this);
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return PasswordResetRequested.class;
  }

  @Override
  public void handleEvent(PasswordResetRequested event) {
    jobScheduler.enqueue(
        () -> notifyUserWithInstructions(event.userId().get()));
  }

  @Job(name = "Notify user with password reset instructions")
  public void notifyUserWithInstructions(String userId) {
    userRepository.findById(UserId.from(userId)).ifPresentOrElse(this::notifyUser, () -> {
      throw new RuntimeException("User with id %s not found".formatted(userId));
    });
  }

  private void notifyUser(User user) {
    emailService.send(new Subject("Please reset your password"),
        new Recipient(user.emailAddress().get(), user.fullName().get()),
        new Content(Messages.formatPasswordResetEmailContent(user.fullName().get(),
            passwordResetLinkSupplier.passwordResetUrl(user.id().get()))));
  }
}
