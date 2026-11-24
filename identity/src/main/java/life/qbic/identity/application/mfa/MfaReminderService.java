package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.application.communication.CommunicationException;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.model.mfa.MfaGrace;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.model.mfa.MfaReminder;
import life.qbic.identity.domain.repository.MfaGraceRepository;
import life.qbic.identity.domain.repository.SecondFactorRepository;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>Sends the two reminders that keep a deadline from arriving as a surprise</b>
 *
 * <p>A grace window is the one deadline in this application whose expiry is not self-explanatory: the
 * user was told once, in a banner, on a day they were busy. Two mails — about a day before, and a couple
 * of hours before — are the cheapest way to shrink the population that ever needs the recovery path.</p>
 *
 * <h3>The rules, in the order they are applied</h3>
 * <ol>
 *   <li><b>No factor required → no reminder.</b> Under {@code OFF} there is no deadline, and a mail
 *       saying "two-factor becomes mandatory" would be false.</li>
 *   <li><b>The window must still be running.</b> After the deadline the correct message is the recovery
 *       path, not a reminder about a date that has passed ({@code MfaReminder}).</li>
 *   <li><b>Already enrolled → skip, and say nothing.</b> The user did what the mail asks; a reminder
 *       that arrives afterwards is the application not noticing.</li>
 *   <li><b>One reminder per stage</b>, recorded on the window, so a restart or a second instance cannot
 *       duplicate it.</li>
 * </ol>
 *
 * <p>A failure to send is logged and counted, never thrown: one account's mail server problem must not
 * stop the pass for everyone else. The stamp is only written after a successful send, so a failed
 * reminder is retried on the next pass.</p>
 *
 * @since 1.0.0
 */
@Service
public class MfaReminderService {

  private static final Logger log = logger(MfaReminderService.class);

  private static final DateTimeFormatter DEADLINE =
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
          .withZone(ZoneId.systemDefault());

  private final MfaGraceRepository graceRepository;
  private final SecondFactorRepository secondFactorRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;
  private final MfaPolicy policy;
  private final Duration soonBefore;
  private final Duration finalBefore;
  private final String settingsUrl;

  public MfaReminderService(MfaGraceRepository graceRepository,
      SecondFactorRepository secondFactorRepository,
      UserRepository userRepository,
      EmailService emailService,
      MfaPolicy policy,
      @Value("${qbic.security.mfa.reminders.soon-before:PT24H}") Duration soonBefore,
      @Value("${qbic.security.mfa.reminders.final-before:PT2H}") Duration finalBefore,
      @Value("${qbic.security.mfa.reminders.settings-url:/settings/two-factor}") String settingsUrl) {
    this.graceRepository = requireNonNull(graceRepository, "graceRepository must not be null");
    this.secondFactorRepository = requireNonNull(secondFactorRepository,
        "secondFactorRepository must not be null");
    this.userRepository = requireNonNull(userRepository, "userRepository must not be null");
    this.emailService = requireNonNull(emailService, "emailService must not be null");
    this.policy = requireNonNull(policy, "policy must not be null");
    this.soonBefore = requireNonNull(soonBefore, "soonBefore must not be null");
    this.finalBefore = requireNonNull(finalBefore, "finalBefore must not be null");
    this.settingsUrl = requireNonNull(settingsUrl, "settingsUrl must not be null");
  }

  /**
   * One pass over the windows that have a reminder outstanding.
   *
   * @param now the instant of the pass
   * @return how many reminders were sent, for the log line and for a metrics counter later
   */
  public int sendDueReminders(Instant now) {
    requireNonNull(now, "now must not be null");
    if (policy.mode() == MfaPolicyMode.OFF) {
      return 0;
    }

    List<MfaGrace> candidates = graceRepository.findActiveWithOutstandingReminder(now);
    int sent = 0;
    for (MfaGrace grace : candidates) {
      MfaReminder.Stage stage = MfaReminder.due(grace, now, soonBefore, finalBefore);
      if (stage == MfaReminder.Stage.NONE) {
        continue;
      }
      if (secondFactorRepository.hasActiveFactor(grace.userId())) {
        // Enrolled in the meantime: the mail would be wrong, and the stamps are not the place to
        // record "nothing to do" — the window simply stops being selected once it expires.
        continue;
      }
      Optional<User> user = userById(grace.userId());
      if (user.isEmpty()) {
        // An id that does not resolve, or does not parse: the pass continues. One account's data
        // problem must not silence the reminders for everybody behind it in the loop.
        log.warn("Second-factor reminder skipped: user %s could not be read".formatted(grace.userId()));
        continue;
      }
      if (send(user.get(), grace, stage)) {
        switch (stage) {
          case SOON -> graceRepository.markDueReminderSent(grace.userId(), now);
          case FINAL -> graceRepository.markFinalReminderSent(grace.userId(), now);
          default -> {
            // unreachable: Stage.NONE was filtered above
          }
        }
        sent++;
      }
    }
    if (sent > 0) {
      log.info("Sent %d second-factor enrolment reminder(s)".formatted(sent));
    }
    return sent;
  }

  private Optional<User> userById(String userId) {
    try {
      return userRepository.findById(UserId.from(userId));
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  private boolean send(User user, MfaGrace grace, MfaReminder.Stage stage) {
    try {
      emailService.send(new Subject(subject(stage)),
          new Recipient(user.emailAddress().get(), user.fullName().get()),
          new Content(body(user.fullName().get(), grace, stage)));
      return true;
    } catch (CommunicationException e) {
      log.error("Second-factor %s reminder could not be sent to user %s: %s"
          .formatted(stage, user.id().get(), e.getClass().getSimpleName()));
      return false;
    }
  }

  private static String subject(MfaReminder.Stage stage) {
    return stage == MfaReminder.Stage.FINAL
        ? "A few hours left to set up two-factor authentication"
        : "One day left to set up two-factor authentication";
  }

  private String body(String fullName, MfaGrace grace, MfaReminder.Stage stage) {
    String deadline = DEADLINE.format(grace.graceUntil());
    String lead = stage == MfaReminder.Stage.FINAL
        ? "There are only a few hours left."
        : "There is about one day left.";
    return """
        Hello %s,

        %s to add a second factor to your Data Manager account. From %s you will need a code from
        an authenticator app to sign in, and signing in without one will not be possible.

        Setting it up takes a minute: open the application, go to Settings, then Two-factor
        authentication, and scan the QR code with an authenticator app such as 1Password, Authy or
        Google Authenticator. Store the recovery codes it shows you — they are the only way back in
        if you lose the device.

        %s

        If you cannot install an authenticator app, contact your QBiC support before the deadline
        rather than after it.

        Kind regards,
        QBiC Data Manager
        """.formatted(fullName, lead, deadline, settingsUrl);
  }
}
