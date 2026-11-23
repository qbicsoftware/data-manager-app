package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <b>Runs {@link MfaReminderService} on a schedule</b>
 *
 * <p>Kept to wiring on purpose: the decision belongs to {@code MfaReminder} (pure) and the sending to
 * the service (testable without a clock), so this class has nothing left that could be wrong except the
 * schedule — and a schedule is exactly what a unit test cannot check.</p>
 *
 * <p><b>No distributed lock, and it is not needed.</b> The application can run more than one instance,
 * and two passes may race — but the stamps on the grace window are written with
 * {@code ... AND reminderDueSentAt IS NULL}, so the second writer changes nothing and the first stamp
 * stands. A lock would save duplicated work, not duplicated mail, and it is not worth a dependency in
 * this module. (Where ShedLock is already on the classpath, wrapping {@link #run} is still the cheap way
 * to reduce the duplication.)</p>
 *
 * <p><b>Interval, not cron.</b> A fixed delay with jitter avoids every instance waking at the same
 * second after a restart, and the reminder windows are hours wide — a few minutes of drift costs
 * nothing. Disabled by setting the interval to {@code -1} (Spring then never fires it), which is how a
 * deployment turns the mails off without changing the policy.</p>
 *
 * @since 1.0.0
 */
@Component
public class MfaReminderJob {

  private static final Logger log = logger(MfaReminderJob.class);

  private final MfaReminderService reminderService;
  private final long initialDelayMs;

  public MfaReminderJob(MfaReminderService reminderService,
      @Value("${qbic.security.mfa.reminders.initial-delay-ms:120000}") long initialDelayMs) {
    this.reminderService = requireNonNull(reminderService, "reminderService must not be null");
    this.initialDelayMs = initialDelayMs;
  }

  /**
   * One pass. Exceptions are logged and swallowed here, never propagated: a scheduled method that
   * throws is retried immediately by Spring, which turns a persistent database problem into a hot loop
   * of failing queries.
   */
  @Scheduled(
      fixedDelayString = "${qbic.security.mfa.reminders.interval-ms:300000}",
      initialDelayString = "${qbic.security.mfa.reminders.initial-delay-ms:120000}")
  public void run() {
    try {
      reminderService.sendDueReminders(Instant.now());
    } catch (RuntimeException e) {
      log.error("Second-factor reminder pass failed: %s".formatted(e.getClass().getSimpleName()));
    }
  }

  /**
   * @return the configured initial delay in milliseconds; exposed so a deployment check can assert it
   *     is a sane number rather than a copy-paste of another job
   */
  long initialDelayMs() {
    return initialDelayMs;
  }
}
