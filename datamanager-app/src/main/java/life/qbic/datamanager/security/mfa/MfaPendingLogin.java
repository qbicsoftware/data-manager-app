package life.qbic.datamanager.security.mfa;

import static java.util.Objects.requireNonNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * <b>The state between "password accepted" and "second factor verified"</b>
 *
 * <p>Held in the HTTP session, and it is deliberately <em>not</em> an {@code Authentication}: the whole
 * point of the login challenge is that a password alone establishes nothing (AUTH-R-01). This record
 * says only <em>who</em> is mid-login and since when — it grants no authority, satisfies no
 * {@code isAuthenticated()} check, and is useless to anyone who is not already holding the session
 * cookie that Spring issued after the password check.</p>
 *
 * <p><b>Bound in time.</b> Five minutes is enough to open an authenticator app; a pending login that
 * outlives that is abandoned. The password was verified at that instant, and an indefinite "half
 * logged in" state is exactly the kind of thing an attacker who finds an unlocked browser would
 * like.</p>
 *
 * <p><b>Single use.</b> {@link #clear(HttpSession)} is called when the factor is verified and when the
 * login is abandoned, so a verified challenge cannot be replayed from the same session.</p>
 *
 * @param userId    the account whose password was verified
 * @param email     what the user typed as the login name; needed to reload the principal
 * @param createdAt when the password check succeeded
 * @since 1.0.0
 */
public record MfaPendingLogin(String userId, String email, Instant createdAt)
    implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String SESSION_ATTRIBUTE = "qbic.mfa.pending-login";

  /** How long a pending login may be completed. */
  public static final Duration LIFETIME = Duration.ofMinutes(5);

  public MfaPendingLogin {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(email, "email must not be null");
    requireNonNull(createdAt, "createdAt must not be null");
  }

  /**
   * Remembers the pending login in the session.
   *
   * @param request the login request; a session exists at this point, Spring has just created one
   * @param pending what to remember
   */
  public static void store(HttpServletRequest request, MfaPendingLogin pending) {
    requireNonNull(request, "request must not be null");
    requireNonNull(pending, "pending must not be null");
    request.getSession().setAttribute(SESSION_ATTRIBUTE, pending);
  }

  /**
   * @param session the current session, may be {@code null}
   * @return the pending login if one is stored, regardless of its age
   */
  public static Optional<MfaPendingLogin> from(HttpSession session) {
    if (session == null) {
      return Optional.empty();
    }
    Object value = session.getAttribute(SESSION_ATTRIBUTE);
    return value instanceof MfaPendingLogin pending ? Optional.of(pending) : Optional.empty();
  }

  /**
   * @param session the current session, may be {@code null}
   * @param now     the instant to evaluate against
   * @return the pending login only while it is still completable
   */
  public static Optional<MfaPendingLogin> usableFrom(HttpSession session, Instant now) {
    requireNonNull(now, "now must not be null");
    return from(session).filter(pending -> !pending.isExpiredAt(now));
  }

  /**
   * @param session the session to clear; {@code null} is ignored
   */
  public static void clear(HttpSession session) {
    if (session != null) {
      session.removeAttribute(SESSION_ATTRIBUTE);
    }
  }

  /**
   * @param now the instant to evaluate against
   * @return {@code true} once the pending login is too old to complete
   */
  public boolean isExpiredAt(Instant now) {
    requireNonNull(now, "now must not be null");
    return !now.isBefore(createdAt.plus(LIFETIME));
  }
}
