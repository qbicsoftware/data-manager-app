package life.qbic.identity.domain.model.mfa;

import java.time.Instant;

/**
 * <b>Outcome of the MFA gate</b>
 *
 * <p>What the login path must do with an account that has just presented its first factor. Returned as
 * a value rather than a boolean because three of the five outcomes differ in <em>what to persist</em>
 * or <em>what to tell the user</em>, and collapsing them is how the grace window gets implemented
 * into a lockout (see {@link MfaGate}).</p>
 *
 * @since 1.0.0
 */
public sealed interface MfaGateDecision
    permits MfaGateDecision.NotRequired, MfaGateDecision.Challenge, MfaGateDecision.GraceGranted,
    MfaGateDecision.GraceActive, MfaGateDecision.GraceExpired {

  /**
   * @return {@code true} if the account may not reach an authenticated session without presenting a
   *     factor
   */
  boolean secondFactorRequired();

  /**
   * @return {@code true} if a session may be established with the password alone at this instant
   */
  boolean loginMayProceedWithoutFactor();

  /**
   * Nothing to do: either the policy is {@link MfaPolicyMode#OFF}, or it is
   * {@link MfaPolicyMode#OPTIONAL} and the account has no active factor.
   */
  record NotRequired() implements MfaGateDecision {

    @Override
    public boolean secondFactorRequired() {
      return false;
    }

    @Override
    public boolean loginMayProceedWithoutFactor() {
      return true;
    }
  }

  /**
   * The account has an active factor and must present it. Applies in {@link MfaPolicyMode#OPTIONAL}
   * as well as {@link MfaPolicyMode#REQUIRED}: once a user has enrolled, their login is protected.
   */
  record Challenge() implements MfaGateDecision {

    @Override
    public boolean secondFactorRequired() {
      return true;
    }

    @Override
    public boolean loginMayProceedWithoutFactor() {
      return false;
    }
  }

  /**
   * The policy is {@link MfaPolicyMode#REQUIRED}, the account has no factor, and no window existed —
   * so one is granted now.
   *
   * <p><b>The caller must persist this deadline before continuing.</b> That persistence is what makes
   * the next login fall into {@link GraceActive} instead of granting a fresh window on every attempt,
   * and it is the difference between "the user gets 48 hours to enrol" and "the user is never asked
   * again".</p>
   *
   * @param graceUntil the deadline to store
   */
  record GraceGranted(Instant graceUntil) implements MfaGateDecision {

    public GraceGranted {
      if (graceUntil == null) {
        throw new IllegalArgumentException("graceUntil must not be null");
      }
    }

    @Override
    public boolean secondFactorRequired() {
      return false;
    }

    @Override
    public boolean loginMayProceedWithoutFactor() {
      return true;
    }
  }

  /**
   * The policy is {@link MfaPolicyMode#REQUIRED}, the account has no factor, and a window is running.
   *
   * <p>The session is real but restricted: the only thing it may do is enrol (ADR 0011 item 3,
   * "grace buys enrolment only").</p>
   *
   * @param graceUntil the stored deadline
   */
  record GraceActive(Instant graceUntil) implements MfaGateDecision {

    public GraceActive {
      if (graceUntil == null) {
        throw new IllegalArgumentException("graceUntil must not be null");
      }
    }

    @Override
    public boolean secondFactorRequired() {
      return false;
    }

    @Override
    public boolean loginMayProceedWithoutFactor() {
      return true;
    }
  }

  /**
   * The policy is {@link MfaPolicyMode#REQUIRED}, the account has no factor, and the window has run
   * out.
   *
   * <p>Deny, and offer recovery: an administrator re-enrolment (M6-1), or a recovery code if one
   * exists. The deadline is carried so the message can state it without another lookup.</p>
   *
   * @param graceUntil the deadline that has passed
   */
  record GraceExpired(Instant graceUntil) implements MfaGateDecision {

    public GraceExpired {
      if (graceUntil == null) {
        throw new IllegalArgumentException("graceUntil must not be null");
      }
    }

    @Override
    public boolean secondFactorRequired() {
      return true;
    }

    @Override
    public boolean loginMayProceedWithoutFactor() {
      return false;
    }
  }
}
