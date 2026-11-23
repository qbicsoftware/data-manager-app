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
    MfaGateDecision.GraceActive, MfaGateDecision.GraceExpired, MfaGateDecision.EnrolmentDue {

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
   * The policy is {@link MfaPolicyMode#REQUIRED}, the account has no factor, and it is <b>not eligible
   * for a grace window</b>: it was created after the posture came into force, so no deadline is owed to
   * it.
   *
   * <h3>Why this is not a grant with a zero-length window</h3>
   * <p>Grace exists for one thing — nobody is locked out by the <em>flip</em> to the required posture
   * (ADR 0011 item 3). An account created afterwards cannot be locked out by a change that preceded it;
   * it is being onboarded into a world that already asks for a factor. Giving it a countdown would
   * invite exactly the failure the countdown would then punish: 48 hours in which nothing in the
   * application insists, followed by a lockout with no way back.</p>
   *
   * <p><b>Nothing is persisted for this outcome.</b> No deadline row is created, so nothing can expire,
   * and the next login asks the same question and gets the same answer until a factor exists. The login
   * path must therefore send the user to enrolment, never to a session.</p>
   */
  record EnrolmentDue() implements MfaGateDecision {

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
   * The policy is {@link MfaPolicyMode#REQUIRED}, the account has no factor, and the window has run
   * out.
   *
   * <p>Deny, and offer recovery: the mailbox-proof re-enrolment link, or a recovery code if one exists
   * (an account that never enrolled has no codes — that is what makes the link the primary path here).
   * The deadline is carried so the message can state it without another lookup.</p>
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
