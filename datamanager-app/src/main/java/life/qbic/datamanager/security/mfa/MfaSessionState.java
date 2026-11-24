package life.qbic.datamanager.security.mfa;

import static java.util.Objects.requireNonNull;

import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * <b>Marks a session whose login completed the second factor</b>
 *
 * <p>One session, one answer: either this session's login has passed the factor (or did not need to),
 * or it has not. Without the marker, {@link MfaChallengeEnforcementFilter} would have to ask the
 * database on <em>every</em> request — two queries per page load for a question whose answer cannot
 * change during the session.</p>
 *
 * <p><b>Bound to the account.</b> The marker stores the user id and is only honoured for that
 * principal, so a session that somehow changes identity cannot inherit an earlier "satisfied" answer.
 * It dies with the session, which is also what logout does.</p>
 *
 * @param userId      the account the answer belongs to
 * @param satisfiedAt when the factor step completed
 * @since 1.0.0
 */
public record MfaSessionState(String userId, Instant satisfiedAt) implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String SESSION_ATTRIBUTE = "qbic.mfa.satisfied";

  public MfaSessionState {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(satisfiedAt, "satisfiedAt must not be null");
  }

  /**
   * Records that this session may be used by the given account.
   *
   * @param session the session; a {@code null} session is ignored because there is nothing to mark
   * @param userId  the account
   * @param now     when the factor step completed
   */
  public static void store(HttpSession session, String userId, Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(now, "now must not be null");
    if (session != null) {
      session.setAttribute(SESSION_ATTRIBUTE, new MfaSessionState(userId, now));
    }
  }

  /**
   * @param session the session, may be {@code null}
   * @param userId  the authenticated account
   * @return {@code true} only if this session completed the factor step for that very account
   */
  public static boolean satisfies(HttpSession session, String userId) {
    if (session == null || userId == null) {
      return false;
    }
    return session.getAttribute(SESSION_ATTRIBUTE) instanceof MfaSessionState state
        && state.userId().equals(userId);
  }

  /**
   * @param session the session to clear; {@code null} is ignored
   */
  public static void clear(HttpSession session) {
    if (session != null) {
      session.removeAttribute(SESSION_ATTRIBUTE);
    }
  }
}
