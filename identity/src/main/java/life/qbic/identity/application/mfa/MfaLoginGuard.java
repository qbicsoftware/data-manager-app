package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import life.qbic.identity.domain.model.mfa.MfaGate;
import life.qbic.identity.domain.model.mfa.MfaGateDecision;
import life.qbic.identity.domain.model.mfa.MfaGrace;
import life.qbic.identity.domain.repository.MfaGraceRepository;
import life.qbic.identity.domain.repository.SecondFactorRepository;
import life.qbic.logging.api.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>The question the login path asks after a password succeeded</b>
 *
 * <p>Wraps {@link MfaGate} with the two things a pure function cannot do: read whether the account has
 * an active factor, and persist a granted grace window. Everything security-relevant stays in the
 * domain decision; this class only supplies its inputs and writes the deadline it asks for.</p>
 *
 * <p><b>Granting is persisted here, in its own transaction.</b> The caller gets
 * {@link MfaGateDecision.GraceGranted} back and lets the login continue — the deadline is already
 * stored, so the next login resolves to {@code GraceActive} instead of granting another window. If the
 * write fails, the exception propagates and the login does not proceed: a session that no deadline
 * covers must not be created, or grace would depend on a silent retry.</p>
 *
 * @since 1.0.0
 */
@Service
public class MfaLoginGuard {

  private static final Logger log = logger(MfaLoginGuard.class);

  private final SecondFactorRepository secondFactorRepository;
  private final MfaGraceRepository graceRepository;
  private final MfaPolicy policy;
  private final GraceEligibility graceEligibility;

  public MfaLoginGuard(SecondFactorRepository secondFactorRepository,
      MfaGraceRepository graceRepository, MfaPolicy policy, GraceEligibility graceEligibility) {
    this.secondFactorRepository = requireNonNull(secondFactorRepository,
        "secondFactorRepository must not be null");
    this.graceRepository = requireNonNull(graceRepository, "graceRepository must not be null");
    this.policy = requireNonNull(policy, "policy must not be null");
    this.graceEligibility = requireNonNull(graceEligibility,
        "graceEligibility must not be null");
  }

  /**
   * Decides what the login path must do for an account whose password has just been verified.
   *
   * @param userId the account
   * @param now    the instant of the decision
   * @return what to do; a {@code GraceGranted} outcome has already been persisted when returned
   */
  @Transactional
  public MfaGateDecision decide(String userId, Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(now, "now must not be null");

    MfaGateDecision decision = MfaGate.decide(
        policy.mode(),
        secondFactorRepository.hasActiveFactor(userId),
        graceRepository.findByUserId(userId),
        now,
        policy.grace(),
        graceEligibility.isEligible(userId));

    // One line per password login, naming the outcome. Diagnosing "no challenge appeared" needs the
    // decision, not the absence of one.
    log.info("Second-factor gate for user %s: %s (mode=%s, grace=%s)"
        .formatted(userId, decision.getClass().getSimpleName(), policy.mode(), policy.grace()));

    if (decision instanceof MfaGateDecision.GraceGranted granted) {
      // First write wins in the adapter, so two concurrent logins cannot move the deadline.
      graceRepository.startIfAbsent(MfaGrace.start(userId, now, policy.grace()));
      log.info("Second-factor grace window granted to user %s until %s"
          .formatted(userId, granted.graceUntil()));
    }
    return decision;
  }

}
