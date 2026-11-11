package life.qbic.identity.application.user.policy.directive;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.api.VerificationLinkService;
import life.qbic.identity.api.VerificationLinkService.Purpose;
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
import java.time.Instant;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When a user registered, email the user a confirmation link to the provided email address.
 *
 * <p>The link carries a single-use, time-limited token (AUTH-R-05), never the user's persistent
 * identifier. Only the token's hash is stored; the raw value exists once, in the outgoing email.</p>
 *
 * <p>Issuing goes through {@link VerificationLinkService} rather than writing the token directly, so
 * "register" and "resend" share one implementation of the supersede-then-save ordering. Two copies
 * of that ordering would eventually disagree.</p>
 *
 * @since 1.0.0
 */
public class WhenUserRegisteredSendConfirmationEmail implements
    DomainEventSubscriber<UserRegistered> {

  private final EmailService emailService;
  private final JobScheduler jobScheduler;
  private final UserRepository userRepository;
  private final EmailConfirmationLinkSupplier emailConfirmationLinkSupplier;
  private final VerificationLinkService verificationLinkService;

  public WhenUserRegisteredSendConfirmationEmail(
      @Autowired EmailService emailService,
      @Autowired JobScheduler jobScheduler,
      @Autowired UserRepository userRepository,
      @Autowired EmailConfirmationLinkSupplier emailConfirmationLinkSupplier,
      @Autowired VerificationLinkService verificationLinkService) {
    this.emailService = requireNonNull(emailService);
    this.jobScheduler = requireNonNull(jobScheduler);
    this.userRepository = requireNonNull(userRepository);
    this.emailConfirmationLinkSupplier = requireNonNull(emailConfirmationLinkSupplier);
    this.verificationLinkService = requireNonNull(verificationLinkService);
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
    // Issue before sending: if issuing fails, no email goes out and the user can request a new link.
    // The reverse order could deliver a link whose token was never persisted.
    VerificationLinkService.Issued issued = verificationLinkService.issue(
        user.id().get(), Purpose.EMAIL_CONFIRMATION, Instant.now());

    emailService.send(new Subject("Please confirm your email address"),
        new Recipient(user.emailAddress().get(), user.fullName().get()),
        new Content(Messages.formatRegistrationEmailContent(user.fullName().get(),
            emailConfirmationLinkSupplier.emailConfirmationUrl(issued.rawToken()))));
  }
}
