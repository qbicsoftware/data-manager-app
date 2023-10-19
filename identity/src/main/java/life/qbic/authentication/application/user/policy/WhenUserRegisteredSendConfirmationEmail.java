package life.qbic.authentication.application.user.policy;

import life.qbic.authentication.application.communication.Messages;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.event.UserRegistered;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
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

  private final CommunicationService communicationService;

  private final JobScheduler jobScheduler;
  private final UserRepository userRepository;
  private final EmailConfirmationLinkSupplier emailConfirmationLinkSupplier;

  public WhenUserRegisteredSendConfirmationEmail(
      @Autowired CommunicationService communicationService,
      @Autowired JobScheduler jobScheduler, @Autowired UserRepository userRepository,
      @Autowired EmailConfirmationLinkSupplier emailConfirmationLinkSupplier) {
    this.communicationService = communicationService;
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
    communicationService.send(new Subject("Please confirm your email address"),
        new Recipient(user.emailAddress().get(), user.fullName().get()),
        new Content(Messages.formatRegistrationEmailContent(user.fullName().get(),
            emailConfirmationLinkSupplier.emailConfirmationUrl(user.id().get()))));
  }
}
