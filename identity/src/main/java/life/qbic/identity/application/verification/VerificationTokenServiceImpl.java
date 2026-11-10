package life.qbic.identity.application.verification;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import life.qbic.identity.api.VerificationLinkService;
import life.qbic.identity.api.VerificationLinkService.Purpose;
import life.qbic.identity.domain.model.verification.TokenPurpose;
import life.qbic.identity.domain.model.verification.VerificationToken;
import life.qbic.identity.domain.model.verification.VerificationTokens;
import life.qbic.identity.domain.repository.VerificationTokenRepository;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Verification token service</b>
 *
 * <p>Issues and consumes the single-use tokens behind email confirmation and password reset
 * (AUTH-R-05).</p>
 *
 * <h3>Failure modes are deliberately uniform</h3>
 * <p>{@link #consume(String, Purpose, Instant)} returns the owning user id or empty. It does
 * <b>not</b> distinguish "unknown token" from "expired" from "already used" in the value it hands
 * back, and the caller must not turn that distinction into user-visible wording either: AUTH-NFR-03
 * requires that an unauthenticated response does not disclose account state. The distinction is
 * logged, never shown.</p>
 *
 * <h3>Transaction boundary</h3>
 * <p>{@code consume} is {@link Transactional} because consumption <em>must</em> be one atomic unit of
 * work. This is not a style choice: with the lookup and the update in separate transactions, the row
 * lock is released at the first commit, the entity becomes detached, and every concurrent consumer
 * reads a stale {@code consumedAt = NULL} and succeeds. That failure was measured (8 of 8 consumers
 * won) — see {@code VerificationTokenConcurrencySpec}.</p>
 *
 * @since 1.0.0
 */
@Service
public class VerificationTokenServiceImpl implements VerificationLinkService {

  private static final Logger log = LoggerFactory.logger(VerificationTokenServiceImpl.class);

  private final VerificationTokenRepository repository;
  private final Duration confirmationLifetime;
  private final Duration resetLifetime;
  private final Duration setPasswordLifetime;

  public VerificationTokenServiceImpl(VerificationTokenRepository repository,
      @Value("${qbic.verification-token.confirmation-lifetime:PT1H}") Duration confirmationLifetime,
      @Value("${qbic.verification-token.reset-lifetime:PT5M}") Duration resetLifetime,
      @Value("${qbic.verification-token.set-password-lifetime:PT1H}") Duration setPasswordLifetime) {
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
   * Enforces the upper bounds stated in AUTH-R-05 at startup.
   *
   * <p>The requirement fixes the <em>maximum</em> lifetime as a security property — it bounds how
   * long an intercepted link stays usable — while leaving the configured value to deployment. A
   * deployment that configures longer must fail at startup rather than silently exceed the
   * requirement.</p>
   */
  private void enforceRequirementBounds() {
    requireWithin(confirmationLifetime, Duration.ofHours(1), "confirmation link", "AUTH-R-05");
    requireWithin(resetLifetime, Duration.ofMinutes(15), "password reset link", "AUTH-R-05");
    requireWithin(setPasswordLifetime, Duration.ofHours(1), "set-password invitation",
        "AUTH-R-05 (by analogy with email confirmation)");
  }

  private static void requireWithin(Duration actual, Duration max, String what, String source) {
    if (actual.compareTo(max) > 0) {
      throw new IllegalArgumentException(
          "%s lifetime must not exceed %s (%s), but was %s".formatted(what, max, source, actual));
    }
  }

  private Duration lifetimeFor(TokenPurpose purpose) {
    return switch (purpose) {
      case EMAIL_CONFIRMATION -> confirmationLifetime;
      case PASSWORD_RESET -> resetLifetime;
      case SET_PASSWORD -> setPasswordLifetime;
    };
  }

  @Override
  @Transactional
  public Issued issue(String userId, Purpose purpose, Instant issuedAt) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(purpose, "purpose must not be null");
    requireNonNull(issuedAt, "issuedAt must not be null");
    TokenPurpose tokenPurpose = TokenPurpose.valueOf(purpose.name());

    // Serialise per account BEFORE touching the token table.
    //
    // Without this, supersede-then-insert is unsafe under concurrency: the UPDATE only sees rows
    // committed before this transaction's snapshot, so concurrent reissues each supersede a
    // different subset and each insert their own token. Measured on MariaDB 12.3: 6 concurrent
    // reissues produced "Deadlock found when trying to get lock" on the INSERT, and on H2 the same
    // 6 left 6 simultaneously usable tokens. Taking the account row lock first makes the sequence
    // behave as if it were sequential.
    repository.lockAccount(userId);

    String rawToken = VerificationTokens.newRawToken();
    String hash = VerificationTokens.hash(rawToken);
    VerificationToken token = VerificationToken.issue(
        userId, tokenPurpose, hash, issuedAt, lifetimeFor(tokenPurpose));

    // Order matters: supersede first, then persist the replacement. Persisting first would leave a
    // window in which both the old and the new token are usable, defeating supersede-on-reissue.
    // Superseding first means a crash between the two leaves the user with no usable token — they
    // request a new one. Fail closed.
    int superseded = repository.supersedeAllFor(userId, tokenPurpose, hash, issuedAt);
    repository.save(token);

    log.info("Issued %s token for user %s (superseded %d previous)"
        .formatted(tokenPurpose, userId, superseded));
    return new Issued(rawToken, token.expiresAt());
  }

  @Override
  @Transactional
  public Optional<String> consume(String rawToken, Purpose purpose, Instant now) {
    requireNonNull(purpose, "purpose must not be null");
    return consume(rawToken, TokenPurpose.valueOf(purpose.name()), now);
  }

  private Optional<String> consume(String rawToken, TokenPurpose tokenPurpose, Instant now) {
    requireNonNull(now, "now must not be null");
    if (rawToken == null || rawToken.isBlank()) {
      return Optional.empty();
    }

    String hash = VerificationTokens.hash(rawToken);
    VerificationTokenRepository.ConsumeResult result =
        repository.consumeIfUsable(hash, tokenPurpose, now);

    return switch (result) {
      case VerificationTokenRepository.Consumed c -> Optional.of(c.token().userId());
      case VerificationTokenRepository.Refused r -> {
        // Logged only. Never user-facing: distinguishing "expired" from "unknown" would disclose
        // account state, which AUTH-NFR-03 forbids.
        log.warn("Token consumption rejected for purpose %s: %s".formatted(tokenPurpose, r.failure()));
        yield Optional.empty();
      }
    };
  }

  @Override
  public String toString() {
    return "VerificationTokenServiceImpl{lifetimes=%s}"
        .formatted(Arrays.asList(confirmationLifetime, resetLifetime, setPasswordLifetime));
  }
}
