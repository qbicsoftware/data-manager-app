package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * <b>Enforcement grace window</b>
 *
 * <p>The per-account window during which an unenrolled user may still log in while the
 * {@code MfaPolicyMode.REQUIRED} policy is in force (ADR 0011 item 3). It exists for exactly one
 * reason: <b>switching the policy on must not deny anyone who has not been given a deadline.</b> The
 * configuration change happens on a running service, and the population that would be denied is
 * everyone who has not enrolled — including users who are on holiday, who did not read the
 * announcement, or who are mid-enrolment.</p>
 *
 * <p><b>The absence of this row means "grant a window", never "deny".</b> Reading the absence as
 * "no grace" is the lockout bug: it denies the entire population at the instant of the configuration
 * change. This is why {@link MfaGate} returns a decision that says <em>what to persist</em> instead of
 * a boolean, and why the repository's `startIfAbsent` is a first-write-wins insert:</p>
 *
 * <ul>
 *   <li>the window starts at the account's first login under {@code REQUIRED}, not at deployment
 *       time, so a user returning from three weeks away still gets a full window;</li>
 *   <li>it is never extended by use — grace buys enrolment, nothing else (AUTH-R-01);</li>
 *   <li>it expires by comparison against an {@link Instant} in Java, never against SQL {@code NOW()},
 *       because every column is {@code datetime(6)} with no timezone and a SQL-side comparison would
 *       silently inherit the database session's zone.</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_mfa_grace")
public class MfaGrace {

  @Id
  @Column(name = "userId", nullable = false)
  private String userId;

  @Column(name = "startedAt", nullable = false)
  private Instant startedAt;

  @Column(name = "graceUntil", nullable = false)
  private Instant graceUntil;

  protected MfaGrace() {
    // for JPA
  }

  private MfaGrace(String userId, Instant startedAt, Instant graceUntil) {
    this.userId = requireNonNull(userId, "userId must not be null");
    if (userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    this.startedAt = requireNonNull(startedAt, "startedAt must not be null");
    this.graceUntil = requireNonNull(graceUntil, "graceUntil must not be null");
    if (!graceUntil.isAfter(startedAt)) {
      throw new IllegalArgumentException(
          "graceUntil must be after startedAt, otherwise the window is empty at the moment it is "
              + "granted (startedAt=" + startedAt + ", graceUntil=" + graceUntil + ")");
    }
  }

  /**
   * Opens a window.
   *
   * @param userId the account
   * @param now    the first login under {@code REQUIRED}
   * @param window how long the window lasts; must be positive
   * @return the new window
   */
  public static MfaGrace start(String userId, Instant now, Duration window) {
    requireNonNull(now, "now must not be null");
    requireNonNull(window, "window must not be null");
    if (window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("grace window must be positive, but was " + window);
    }
    return new MfaGrace(userId, now, now.plus(window));
  }

  public String userId() {
    return userId;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public Instant graceUntil() {
    return graceUntil;
  }

  /**
   * @param now the instant to evaluate against
   * @return {@code true} while the deadline has not been reached — the same boundary rule as
   *     {@code VerificationToken.isExpiredAt}: reaching the deadline counts as expired
   */
  public boolean isActiveAt(Instant now) {
    requireNonNull(now, "now must not be null");
    return now.isBefore(graceUntil);
  }

  /**
   * @param now the instant to evaluate against
   * @return {@code true} once the deadline has been reached
   */
  public boolean isExpiredAt(Instant now) {
    return !isActiveAt(now);
  }
}
