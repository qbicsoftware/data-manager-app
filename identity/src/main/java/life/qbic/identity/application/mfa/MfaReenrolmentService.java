package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.Optional;
import life.qbic.identity.api.VerificationLinkService;
import life.qbic.identity.api.VerificationLinkService.Purpose;
import life.qbic.identity.application.communication.CommunicationException;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.repository.SecondFactorRepository;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.logging.api.Logger;
import org.springframework.stereotype.Service;

/**
 * <b>Getting back in when the enrolment window was missed — without an administrator</b>
 *
 * <p>Before this service, a user whose grace window ran out was locked out for good: the window cannot
 * be reopened (its repository is deliberately first-write-wins, so nobody can renew their own
 * deadline), no administrator has a button that grants one, and the page they landed on told them to
 * ask for exactly that. The only way back was a database update.</p>
 *
 * <h3>Why the mailbox is the right proof here, and only here</h3>
 * <p>For an account with <b>no factor</b>, the mailbox already controls the account: a password reset
 * makes it yours. Proving mailbox control therefore grants nothing that was not already available, and
 * the link <b>cannot finish</b> an enrolment — it only opens the page. Completing it still needs a code
 * from the device being enrolled, so a compromised mailbox cannot install a factor of its own.</p>
 *
 * <p>That reasoning stops at an <b>enrolled</b> account. There a factor exists, so a mailbox-only reset
 * would be the cheapest way <em>past</em> the requirement, and a social-engineering target aimed exactly
 * at the account an attacker wants. Such accounts get no link: their path is identity verification by
 * the helpdesk (M6-1), and this service deliberately cannot help them.</p>
 *
 * <h3>Uniform answers (AUTH-NFR-03)</h3>
 * <p>{@link #request} returns nothing and never says whether the address exists, whether the account
 * has a factor, or whether the mail went out. The caller renders one message for every case, so the page
 * cannot be used to enumerate accounts. The distinction is logged, never shown.</p>
 *
 * @since 1.0.0
 */
@Service
public class MfaReenrolmentService {

  private static final Logger log = logger(MfaReenrolmentService.class);

  private static final String TOO_MANY_REQUESTS =
      "A second-factor enrolment link was requested for user %s more often than a human would; the "
          + "previous link has been replaced";

  private final UserRepository userRepository;
  private final SecondFactorRepository secondFactorRepository;
  private final VerificationLinkService verificationLinkService;
  private final EmailService emailService;
  private final MfaReenrolmentLinkSupplier linkSupplier;
  private final MfaPolicy policy;

  public MfaReenrolmentService(UserRepository userRepository,
      SecondFactorRepository secondFactorRepository,
      VerificationLinkService verificationLinkService,
      EmailService emailService,
      MfaReenrolmentLinkSupplier linkSupplier,
      MfaPolicy policy) {
    this.userRepository = requireNonNull(userRepository, "userRepository must not be null");
    this.secondFactorRepository = requireNonNull(secondFactorRepository,
        "secondFactorRepository must not be null");
    this.verificationLinkService = requireNonNull(verificationLinkService,
        "verificationLinkService must not be null");
    this.emailService = requireNonNull(emailService, "emailService must not be null");
    this.linkSupplier = requireNonNull(linkSupplier, "linkSupplier must not be null");
    this.policy = requireNonNull(policy, "policy must not be null");
  }

  /**
   * Sends a one-time enrolment link to the address, if an account without a factor belongs to it.
   *
   * <p>Never reports what happened — see the class documentation. A mail that cannot be sent is logged
   * and swallowed: the user is told to check their inbox either way, and a different answer for "we
   * could not reach the mail server" would tell an attacker that this address has an account.</p>
   *
   * @param emailAddress the address the user typed or the account they are signed in as
   * @param now          the request instant
   */
  public void request(String emailAddress, Instant now) {
    requireNonNull(emailAddress, "emailAddress must not be null");
    requireNonNull(now, "now must not be null");

    if (policy.mode() == MfaPolicyMode.OFF) {
      // Nothing is required, so there is nothing to re-enrol. Sending a link would imply otherwise.
      log.info("Second-factor enrolment link not sent: the deployment does not ask for a factor");
      return;
    }
    Optional<User> candidate = userByEmail(emailAddress);
    if (candidate.isEmpty()) {
      log.info("Second-factor enrolment link requested for an address that has no account");
      return;
    }
    User user = candidate.get();
    if (secondFactorRepository.hasActiveFactor(user.id().get())) {
      // The deliberate boundary: an enrolled account needs identity proofing, not a mailbox link.
      log.info("Second-factor enrolment link not sent for user %s: the account already has a factor"
          .formatted(user.id().get()));
      return;
    }

    try {
      VerificationLinkService.Issued issued =
          verificationLinkService.issue(user.id().get(), Purpose.REENROL_SECOND_FACTOR, now);
      emailService.send(new Subject("Set up two-factor authentication for your Data Manager account"),
          new Recipient(user.emailAddress().get(), user.fullName().get()),
          new Content(mailBody(user.fullName().get(), linkSupplier.reenrolmentUrl(issued.rawToken()))));
      log.info("Second-factor enrolment link sent to user %s".formatted(user.id().get()));
    } catch (CommunicationException e) {
      log.error("Second-factor enrolment link could not be sent to user %s: %s"
          .formatted(user.id().get(), e.getClass().getSimpleName()));
    } catch (RuntimeException e) {
      // Includes the token-issue failures. Logged without the token, which is a credential.
      log.error("Second-factor enrolment link could not be issued for user %s: %s"
          .formatted(user.id().get(), e.getClass().getSimpleName()));
    }
  }

  /**
   * Redeems a link.
   *
   * @param rawToken the token from the query string
   * @param now      the instant of redemption
   * @return the account the link may start an enrolment for, or empty for <em>any</em> rejection
   *     (unknown, expired, already used, wrong flow, account already enrolled)
   */
  public Optional<ReenrolCandidate> redeem(String rawToken, Instant now) {
    requireNonNull(now, "now must not be null");
    Optional<String> userId = verificationLinkService.consume(rawToken,
        Purpose.REENROL_SECOND_FACTOR, now);
    if (userId.isEmpty()) {
      return Optional.empty();
    }
    return userRepository.findById(life.qbic.identity.domain.model.UserId.from(userId.get()))
        .filter(user -> !secondFactorRepository.hasActiveFactor(user.id().get()))
        .map(user -> new ReenrolCandidate(user.id().get(), user.emailAddress().get()));
  }

  private Optional<User> userByEmail(String emailAddress) {
    try {
      return userRepository.findByEmail(EmailAddress.from(emailAddress));
    } catch (RuntimeException e) {
      // A malformed address is indistinguishable from an unknown one, on purpose.
      log.info("Second-factor enrolment link requested for an address that cannot be parsed (%s)"
          .formatted(e.getClass().getSimpleName()));
      return Optional.empty();
    }
  }

  private static String mailBody(String fullName, String url) {
    return """
        Hello %s,

        this installation requires a second factor when you sign in, and your account does not have
        one yet. The link below opens the setup page, where you can add the Data Manager to your
        authenticator app.

        %s

        The link works once and expires after 15 minutes. Setting the factor up still requires a code
        from the app you are adding, so nobody who only reads this mailbox can finish it.

        If you did not ask for this, you can ignore the mail. Your account is unchanged.

        Kind regards,
        QBiC Data Manager
        """.formatted(fullName, url);
  }

  /**
   * The account a redeemed link belongs to.
   *
   * <p>Only the two values the login path needs to build a pending login: no domain object leaves the
   * application layer for the UI (AGENTS.md, "views must not import domain classes").</p>
   *
   * @param userId       the account
   * @param emailAddress the address to reload the principal with when the enrolment completes
   * @since 1.0.0
   */
  public record ReenrolCandidate(String userId, String emailAddress) {

    public ReenrolCandidate {
      requireNonNull(userId, "userId must not be null");
      requireNonNull(emailAddress, "emailAddress must not be null");
    }
  }
}
