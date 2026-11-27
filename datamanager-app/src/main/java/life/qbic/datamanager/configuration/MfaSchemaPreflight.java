package life.qbic.datamanager.configuration;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * <b>Checks at startup that the second-factor columns the code maps actually exist</b>
 *
 * <p>A missing column in {@code user_mfa_grace} is not a local failure. Hibernate selects every mapped
 * column when it loads the entity, and the login guard loads it on <b>every password login</b> — so a
 * database that was not migrated does not merely break the reminder job, it makes signing in fail for
 * everyone, including accounts that have a factor. That happened: the reminder job was the first to
 * notice, with a warning that named a column and nothing else.</p>
 *
 * <p>So the mismatch is reported once, at boot, with the migration to run. The check is deliberately
 * <b>non-fatal</b>: refusing to start turns a diagnosable deployment problem into an outage that also
 * hides the diagnosis, and an operator may still need the instance up to read the log that says what to
 * do. It is loud, ordered early, and it costs one query with no rows.</p>
 *
 * <p><b>Not a substitute for the migration.</b> It cannot add the columns, and it does not retry: fix the
 * schema, then restart (or wait — the login path recovers by itself as soon as the column exists,
 * because nothing is cached).</p>
 *
 * @since 1.0.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MfaSchemaPreflight implements ApplicationRunner {

  private static final Logger log = logger(MfaSchemaPreflight.class);

  /**
   * The columns this code maps on {@code user_mfa_grace}. Kept as one statement: a query that selects
   * them and returns nothing proves both the table and the columns in one round trip.
   */
  /**
   * Probes for {@code user_second_factor.lastAcceptedStep}, the replay-protection mark (MFA-20).
   *
   * <p>Separate from the grace-column probe because it answers a different question: a deployment that
   * applied the first draft of this migration but not this column would fail every login with an
   * {@code Unknown column} error, which is a support call rather than a security incident — but it is
   * cheaper to say so at boot.</p>
   */
  static final String REPLAY_COLUMN_PROBE =
      "SELECT `lastAcceptedStep` FROM `user_second_factor` WHERE 1 = 0";

  static final String COLUMN_PROBE =
      "SELECT reminderDueSentAt, reminderFinalSentAt FROM user_mfa_grace WHERE 1 = 0";

  static final String MIGRATION_HINT =
      "Apply the reminder columns from docs/migrations/NEXT.md before this build takes traffic: "
          + "ALTER TABLE `user_mfa_grace` ADD COLUMN `reminderDueSentAt` datetime(6) NULL, "
          + "ADD COLUMN `reminderFinalSentAt` datetime(6) NULL. "
          + "Until then every password login fails, not only the reminder job.";

  private final transient EntityManager entityManager;

  public MfaSchemaPreflight(EntityManager entityManager) {
    this.entityManager = requireNonNull(entityManager, "entityManager must not be null");
  }

  static final String REPLAY_COLUMN_HINT =
      "Apply the replay-protection column before this build takes traffic: "
          + "ALTER TABLE user_second_factor ADD COLUMN lastAcceptedStep bigint(20) NULL. "
          + "Until then every second-factor verification fails with an unknown-column error.";

  @Override
  public void run(ApplicationArguments args) {
    probeReplayColumn();
    try {
      entityManager.createNativeQuery(COLUMN_PROBE).getResultList();
      log.info("Second-factor schema check passed: user_mfa_grace has the reminder columns");
    } catch (RuntimeException e) {
      Optional<String> hint = hintFor(e);
      if (hint.isPresent()) {
        log.error(hint.get());
      } else {
        // A different failure (connection, permissions): worth knowing, but not this class's diagnosis
        // to make, and definitely not evidence that a migration is missing.
        log.warn("Second-factor schema could not be verified (%s); if logins fail, check the "
            .formatted(e.getClass().getSimpleName())
            + "user_mfa_grace migration in docs/migrations/NEXT.md");
      }
    }
  }

  /**
   * Reports a missing replay-protection column (MFA-20) separately from the grace columns.
   *
   * <p>Two probes rather than one because the failures mean different things and point at different
   * migrations: the grace columns gate <em>every</em> password login, while a missing
   * {@code lastAcceptedStep} breaks only second-factor verification. Merging them would make the hint
   * name the wrong script.</p>
   */
  private void probeReplayColumn() {
    try {
      entityManager.createNativeQuery(REPLAY_COLUMN_PROBE).getResultList();
      log.info("Second-factor schema check passed: user_second_factor has lastAcceptedStep");
    } catch (RuntimeException e) {
      if (hintFor(e).isPresent()) {
        log.error(REPLAY_COLUMN_HINT);
      } else {
        log.warn("Second-factor replay column could not be verified (%s)"
            .formatted(e.getClass().getSimpleName()));
      }
    }
  }

  /**
   * Recognises the one failure this class can name: a column the entity maps but the table lacks.
   *
   * <p>Matched by SQL state ({@code 42S22}, "column not found") with the message as a fallback, because
   * the driver wraps it twice and the state survives the wrapping while the exception type does not.</p>
   *
   * @param failure what the probe threw
   * @return the actionable message, or empty when this is not a missing column
   */
  static Optional<String> hintFor(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      if (current instanceof SQLException sqlException
          && "42S22".equals(sqlException.getSQLState())) {
        return Optional.of(MIGRATION_HINT);
      }
      String message = current.getMessage();
      if (message != null && message.contains("Unknown column")) {
        return Optional.of(MIGRATION_HINT);
      }
      if (current == current.getCause()) {
        break;
      }
      current = current.getCause();
    }
    return Optional.empty();
  }

  /**
   * @return {@code true} when the given failure means "the schema is older than the code"
   */
  static boolean isMissingColumn(PersistenceException failure) {
    return hintFor(failure).isPresent();
  }
}
