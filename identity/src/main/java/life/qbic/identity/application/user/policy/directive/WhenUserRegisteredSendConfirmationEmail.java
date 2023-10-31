package life.qbic.identity.application.user.policy.directive;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Messages;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier;
import life.qbic.identity.domain.event.UserRegistered;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * When a user registered, email the user a confirmation link to the provided email address.
 *
 * @since 1.0.0
 */
@Component
public class WhenUserRegisteredSendConfirmationEmail implements
    DomainEventSubscriber<UserRegistered> {

  private final EmailService emailService;

  private final JobScheduler jobScheduler;
  private final UserRepository userRepository;
  private final EmailConfirmationLinkSupplier emailConfirmationLinkSupplier;

  public WhenUserRegisteredSendConfirmationEmail(
      @Autowired EmailService emailService,
      @Autowired JobScheduler jobScheduler, @Autowired UserRepository userRepository,
      @Autowired EmailConfirmationLinkSupplier emailConfirmationLinkSupplier) {
    this.emailService = emailService;
    this.userRepository = userRepository;
    this.emailConfirmationLinkSupplier = emailConfirmationLinkSupplier;
    DomainEventDispatcher.instance().subscribe(this);
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return UserRegistered.class;
  }

  @Override
  public void handleEvent(UserRegistered event) {
    this.jobScheduler.enqueue(() -> notifyUserWithConfirmationInstructions(event.userId()));
  }

  @Job(name = "Notify user with account confirmation instructions")
  public void notifyUserWithConfirmationInstructions(String userId) {
    userRepository.findById(UserId.from(userId)).ifPresentOrElse(this::notifyUser, () -> {
      throw new RuntimeException("User with id %s not found".formatted(userId));
    });
  }

  private void notifyUser(User user) {
    emailService.send(new Subject("Please confirm your email address"),
        new Recipient(user.emailAddress().get(), user.fullName().get()),
        new Content(Messages.formatRegistrationEmailContent(user.fullName().get(),
            emailConfirmationLinkSupplier.emailConfirmationUrl(user.id().get()))));
  }
}
