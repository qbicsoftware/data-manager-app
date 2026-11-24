package life.qbic.identity.domain.model.mfa;

/**
 * <b>MFA policy mode</b>
 *
 * <p>The deployment-wide posture towards second factors (ADR 0011 P1). It is configuration, not
 * database state: a change is a reviewed configuration diff that appears in the release note, and
 * <em>who is exempt</em> is never data. The only per-account deviation is a
 * {@link MfaGrace} window, which expires by itself.</p>
 *
 * <p>Posture is deliberately per <em>deployment</em> rather than per cohort in this milestone, which
 * is what makes {@link #REQUIRED} a single, testable rule ("no exemption set is empty"). An
 * administrator surface for setting it per account is a future, tightening-only slice (ADR 0011
 * item 6).</p>
 *
 * @since 1.0.0
 */
public enum MfaPolicyMode {

  /**
   * Second factors are not part of login at all.
   *
   * <p>A kill switch, and it means exactly that: users who have enrolled are <em>not</em> challenged
   * either. That is intentional — the mode exists so an incident or a broken factor store can be
   * taken out of the login path by a configuration change, and a half-on mode would leave the broken
   * code reachable. It must only ever be used as a temporary posture, never as the steady state once
   * {@link #REQUIRED} has been announced.</p>
   */
  OFF,

  /**
   * Users decide. Those who enrolled are challenged; those who did not are not.
   *
   * <p>The pilot posture (milestone M7) and the state during the announced enrolment period. Note the
   * asymmetry: an enrolled user is always challenged, otherwise enrolling would be a downgrade.</p>
   */
  OPTIONAL,

  /**
   * Every account must present a second factor, with the {@link MfaGrace} window as the only way an
   * unenrolled account may still log in — and only to enrol.
   *
   * <p>This is the milestone's end state (AUTH-R-01): after the grace window, a password alone yields
   * no session, and no account is exempt.</p>
   */
  REQUIRED
}
