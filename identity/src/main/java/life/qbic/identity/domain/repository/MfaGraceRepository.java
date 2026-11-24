package life.qbic.identity.domain.repository;

import java.time.Instant;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.MfaGrace;

/**
 * <b>MFA grace window repository</b>
 *
 * <p>Persistence port for {@link MfaGrace}. Implemented in {@code identity-infrastructure}.</p>
 *
 * @since 1.0.0
 */
public interface MfaGraceRepository {

  /**
   * @param userId the account
   * @return the stored window, or empty if the account has never been subject to
   *     {@code REQUIRED}. Empty means "grant one", not "deny" — see {@code MfaGate}.
   */
  Optional<MfaGrace> findByUserId(String userId);

  /**
   * Opens a window if the account has none, and does nothing otherwise.
   *
   * <p><b>First write wins, atomically.</b> Two logins of the same account at the same moment must not
   * both grant a window: a read-then-write would let the second one overwrite the first deadline,
   * and — worse — the naive "delete and re-insert" variant would let anyone extend their own window by
   * logging in twice, turning the one deadline the whole enforcement depends on into an
   * indefinitely renewable one. The adapter therefore performs an insert-if-absent in a single
   * statement and returns whatever is stored afterwards.</p>
   *
   * @param candidate the window to store if none exists
   * @return the stored window — the candidate if this call was the first, otherwise the existing one
   */
  MfaGrace startIfAbsent(MfaGrace candidate);

  /**
   * Every window that is still running and has at least one reminder outstanding.
   *
   * <p>Filtering in the query, not in memory: a deployment with thousands of accounts must not load
   * every window once a minute. "Outstanding" means a null stamp — the same fact {@code MfaReminder}
   * uses, read in the place that can do it cheaply.</p>
   *
   * @param now the instant of the pass
   * @return the windows the reminder job has to consider
   */
  java.util.List<MfaGrace> findActiveWithOutstandingReminder(Instant now);

  /**
   * Records that the first reminder went out.
   *
   * <p>Written after the mail is handed to the mail service, and never rolled back: a reminder that was
   * sent but not recorded is one duplicate mail, while a recorded reminder that was not sent is a user
   * who never hears about the deadline. The failure mode is chosen deliberately.</p>
   *
   * @param userId the account
   * @param now    when it was sent
   */
  void markDueReminderSent(String userId, Instant now);

  /**
   * Records that the final reminder went out (see {@link #markDueReminderSent}).
   *
   * @param userId the account
   * @param now    when it was sent
   */
  void markFinalReminderSent(String userId, Instant now);
}
