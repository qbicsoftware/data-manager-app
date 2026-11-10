package life.qbic.identity.application.verification;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import life.qbic.identity.domain.model.verification.ConsumeFailure;
import life.qbic.identity.domain.model.verification.TokenPurpose;
import life.qbic.identity.domain.model.verification.VerificationToken;
import life.qbic.identity.domain.model.verification.VerificationTokens;
import life.qbic.identity.domain.repository.VerificationTokenRepository;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b>Verification token service</b>
 *
 * <p>Issues and consumes the single-use tokens behind email confirmation, password reset and the
 * set-password invitation (AUTH-R-05).</p>
 *
 * <h3>Failure modes are deliberately uniform</h3>
 * <p>{@link #consume(String, TokenPurpose)} returns a {@link Consumed} or an
 * {@link Invalid}. It does <b>not</b> distinguish "unknown token" from "expired token" from
 * "already used" in the type it hands back to the caller, and the caller must not turn that
 * distinction into user-visible wording either: AUTH-NFR-03 requires that an unauthenticated
 * response does not disclose account state. The distinction is kept here only so the server can log
 * it; the user-facing message is always "this link is no longer valid, request a new one".</p>
 *
 * <h3>Concurrency</h3>
 * <p>Two requests may present the same token at the same time (double click, mail-scanner prefetch
 * racing the user, a retried POST). The unique index on {@code tokenHash} makes the database reject
 * the second <em>insert</em>, but consumption is an <em>update</em> — so the second consumer must be
 * stopped by an optimistic or pessimistic lock, not by the index. See
 * {@code VerificationTokenServiceConcurrencySpec} for what was actually measured.</p>
 *
 * @since 1.0.0
 */
public class VerificationTokenServiceImpl {

  private static final Logger log = LoggerFactory.logger(VerificationTokenServiceImpl.class);

  private final VerificationTokenRepository repository;
  private final Duration confirmationLifetime;
  private final Duration resetLifetime;
  private final Duration setPasswordLifetime;

  public VerificationTokenServiceImpl(VerificationTokenRepository repository,
      Duration confirmationLifetime,
      Duration resetLifetime,
      Duration setPasswordLifetime) {
    this.repository = requireNonNull(repository, "repository must not be null");
    this.confirmationLifetime = requirePositive(confirmationLifetime, "confirmationLifetime");
    this.resetLifetime = requirePositive(resetLifetime, "resetLifetime");
    this.setPasswordLifetime = requirePositive(setPasswordLifetime, "setPasswordLifetime");
    enforceRequirementBounds();
  }

  private static Duration requirePositive(Duration duration, String name) {
    requireNonNull(duration, name + " must not be null");
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive, but was " + duration);
    }
    return duration;
  }

  /**
   * Enforces the upper bounds stated in AUTH-R-05.
   *
   * <p>The requirement fixes the <em>maximum</em> lifetime as a security property (it bounds how
   * long an intercepted link stays usable) while leaving the configured value to deployment. A
   * deployment that configures a longer lifetime must fail at startup, not silently exceed the
   * requirement.</p>
   */
  private void enforceRequirementBounds() {
    if (confirmationLifetime.compareTo(Duration.ofHours(1)) > 0) {
      throw new IllegalArgumentException(
          "confirmation link lifetime must not exceed 1 hour (AUTH-R-05), but was "
              + confirmationLifetime);
    }
    if (resetLifetime.compareTo(Duration.ofMinutes(15)) > 0) {
      throw new IllegalArgumentException(
          "password reset link lifetime must not exceed 15 minutes (AUTH-R-05), but was "
              + resetLifetime);
    }
    if (setPasswordLifetime.compareTo(Duration.ofHours(1)) > 0) {
      throw new IllegalArgumentException(
          "set-password invitation lifetime must not exceed 1 hour (AUTH-R-05 by analogy "
              + "with email confirmation), but was " + setPasswordLifetime);
    }
  }

  private Duration lifetimeFor(TokenPurpose purpose) {
    return switch (purpose) {
      case EMAIL_CONFIRMATION -> confirmationLifetime;
      case PASSWORD_RESET -> resetLifetime;
      case SET_PASSWORD -> setPasswordLifetime;
    };
  }

  /**
   * Issues a token, superseding any still-usable token for the same (user, purpose).
   *
   * @param userId   the account
   * @param purpose  the flow
   * @param issuedAt the issuing instant
   * @return the raw token, to be placed in the link. This is the only time it exists in plaintext.
   */
  public Issued issue(String userId, TokenPurpose purpose, Instant issuedAt) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(purpose, "purpose must not be null");
    requireNonNull(issuedAt, "issuedAt must not be null");

    String rawToken = VerificationTokens.newRawToken();
    String hash = VerificationTokens.hash(rawToken);
    VerificationToken token = VerificationToken.issue(
        userId, purpose, hash, issuedAt, lifetimeFor(purpose));

    // Order matters: supersede first, then persist the replacement. Persisting first would leave a
    // window in which both the old and the new token are usable, defeating the point of
    // superseding. Superseding first means a crash between the two calls leaves the user with no
    // usable token — they request a new one. Fail closed.
    int superseded = repository.supersedeAllFor(userId, purpose, hash, issuedAt);
    repository.save(token);

    log.info("Issued %s token for user %s (superseded %d previous)"
        .formatted(purpose, userId, superseded));
    return new Issued(rawToken, token.expiresAt());
  }

  /**
   * Consumes a token.
   *
   * @param rawToken the raw token from the link
   * @param purpose  the flow attempting to consume it
   * @param now      the consumption instant
   * @return {@link Consumed} with the owning user id, or {@link Invalid} with a reason for logging
   */
  public Outcome consume(String rawToken, TokenPurpose purpose, Instant now) {
    requireNonNull(now, "now must not be null");
    if (rawToken == null || rawToken.isBlank()) {
      return new Invalid(ConsumeFailure.MALFORMED);
    }
    if (purpose == null) {
      return new Invalid(ConsumeFailure.WRONG_PURPOSE);
    }

    String hash = VerificationTokens.hash(rawToken);

    // One atomic unit of work. Consumption cannot be split into "look up, then decide, then write":
    // the row lock is released when the reading transaction commits, the entity handed back is
    // detached, and concurrent consumers all win. Measured, not assumed — 8 of 8 succeeded before
    // this was fixed. See VerificationTokenConcurrencySpec.
    VerificationTokenRepository.ConsumeResult result =
        repository.consumeIfUsable(hash, purpose, now);
    return switch (result) {
      case VerificationTokenRepository.Consumed c -> new Consumed(c.token().userId());
      case VerificationTokenRepository.Refused r -> {
        // The user-facing message is identical for every failure mode (AUTH-NFR-03), so the reason
        // is logged only. Distinguishing "unknown" from "expired" to the user would leak state.
        log.warn("Token consumption rejected for purpose %s: %s".formatted(purpose, r.failure()));
        yield new Invalid(r.failure());
      }
    };
  }

  /** Result of {@link #issue}. */
  public record Issued(String rawToken, Instant expiresAt) {

  }

  /** Result of {@link #consume}. */
  public sealed interface Outcome permits Consumed, Invalid {

  }

  /** The token was valid and has now been consumed. */
  public record Consumed(String userId) implements Outcome {

  }

  /**
   * The token was not valid. The reason is for server-side logging only — never for the user.
   *
   * @param reason why consumption failed
   */
  public record Invalid(ConsumeFailure reason) implements Outcome {

  }


}
