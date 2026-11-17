package life.qbic.identity.application.user.policy;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Messages;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.identity.api.VerificationLinkService;
import life.qbic.identity.api.VerificationLinkService.Purpose;
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

  private final VerificationLinkService verificationLinkService;

  public WhenPasswordResetNotifyUser(@Autowired EmailService emailService,
      @Autowired UserRepository userRepository,
      @Autowired JobScheduler jobScheduler,
      @Autowired PasswordResetLinkSupplier passwordResetLinkSupplier,
      @Autowired VerificationLinkService verificationLinkService) {
    this.emailService = emailService;
    this.jobScheduler = jobScheduler;
    this.userRepository = userRepository;
    this.passwordResetLinkSupplier = passwordResetLinkSupplier;
    this.verificationLinkService = verificationLinkService;
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

  /**
   * Delivers the reset mail.
   *
   * <h3>Why this job does not retry</h3>
   *
   * <p>{@code notifyUser} issues a <b>new</b> token on every invocation, and issuing supersedes the
   * previous one (AUTH-R-05). That is the right behaviour for a second <em>user request</em>, but
   * this job is a retry of the <em>same</em> request, so each attempt invalidates the link the
   * previous attempt may already have delivered.</p>
   *
   * <p>The two paths therefore pull in opposite directions:</p>
   * <ul>
   *   <li>If the SMTP server <em>rejected</em> the message, only the last attempt's mail exists and
   *       retrying is harmless.</li>
   *   <li>If the server <em>accepted</em> the message but the connection failed before the client saw
   *       the acknowledgement — the ordinary {@code timeout after DATA} case — the earlier mail is
   *       already queued for delivery and its link is about to be superseded. The recipient then
   *       holds two mails and only the newer link works.</li>
   * </ul>
   *
   * <p>Retrying cannot be made safe here without carrying the raw token across attempts, which would
   * mean persisting it in plaintext — in the JobRunr job arguments, which are stored as JSON in the
   * database. That would break the property {@code VerificationToken} exists to provide: only a hash
   * is stored, and the raw value exists once, inside the email. Doing it properly needs a durable
   * outbox with a short retention and therefore an ADR, which is not a hotfix.</p>
   *
   * <p>So the attempt budget is zero rather than JobRunr's default of 10. The default schedule is
   * {@code 3^n} seconds — attempts at roughly +3 s, +12 s, +39 s, +2 min, +6 min, +18 min, +55 min,
   * …, over <b>24 hours</b> — which also outlives the 5-minute reset token by orders of magnitude, so
   * the later attempts cannot rescue a link anyway.</p>
   *
   * <p>The cost is that a transient SMTP fault leaves the user with no mail. That is the cheaper
   * failure: the request page already says "check your inbox, if an account exists", the user can
   * request another link immediately, and the resend throttle allows three per 15 minutes. Previously
   * the failure was also completely silent; that part is fixed in {@code EmailServiceProvider}, which
   * now logs every submission failure.</p>
   *
   * @see <a href="https://github.com/qbicsoftware/data-manager-app/issues/1674">#1674</a>
   */
  @Job(name = "Notify user with password reset instructions", retries = 0)
  public void notifyUserWithInstructions(String userId) {
    userRepository.findById(UserId.from(userId)).ifPresentOrElse(this::notifyUser, () -> {
      throw new RuntimeException("User with id %s not found".formatted(userId));
    });
  }

  private void notifyUser(User user) {
    // A fresh reset token supersedes any previous one, so requesting a new link cannot be used to
    // keep older links alive (AUTH-R-05).
    VerificationLinkService.Issued issued = verificationLinkService.issue(
        user.id().get(), Purpose.PASSWORD_RESET, java.time.Instant.now());

    emailService.send(new Subject("Please reset your password"),
        new Recipient(user.emailAddress().get(), user.fullName().get()),
        new Content(Messages.formatPasswordResetEmailContent(user.fullName().get(),
            passwordResetLinkSupplier.passwordResetUrl(issued.rawToken()))));
  }
}
