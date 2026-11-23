package life.qbic.identity.application.mfa;

/**
 * <b>Whether an account is owed a grace window at all</b>
 *
 * <p>The last input {@link life.qbic.identity.domain.model.mfa.MfaGate} needs and cannot derive: the
 * grace window belongs to the population that was using the application when the required posture came
 * into force. An account created afterwards is not owed one — it cannot be locked out by a change that
 * preceded it — so it enrols at its first login instead of starting a countdown it may ignore.</p>
 *
 * <p>An interface rather than a method on {@link MfaPolicy}, because answering it needs the account
 * record and the policy together, and neither of those belongs in the other. It is also the seam that
 * keeps the login guard's tests about <em>decisions</em>: the guard asks this question once and never
 * sees a repository.</p>
 *
 * @since 1.0.0
 */
public interface GraceEligibility {

  /**
   * @param userId the account whose first factor was just accepted
   * @return {@code true} if this account may still be granted a grace window
   */
  boolean isEligible(String userId);
}
