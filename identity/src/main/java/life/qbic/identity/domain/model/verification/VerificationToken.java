package life.qbic.identity.domain.model.verification;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * <b>Verification token</b>
 *
 * <p>A single-use, time-limited token backing an email confirmation, password reset or set-password
 * link (AUTH-R-05). Only the SHA-256 hash of the token is persisted; the raw token exists once, in
 * the outgoing email, and cannot be recovered from the database.</p>
 *
 * <p>Three distinct end-states are recorded, because they answer three different support
 * questions:</p>
 * <ul>
 *   <li>{@link #consumedAt()} — the token was used</li>
 *   <li>{@link #supersededAt()} — a newer token was issued for the same (user, purpose)</li>
 *   <li>{@link #expiresAt()} — the token aged out</li>
 * </ul>
 *
 * <p><b>Time handling:</b> every timestamp is a {@link Instant}, and
 * {@link #isUsableAt(Instant)} takes the "now" as a parameter. Expiry is therefore always evaluated
 * in one clock, in Java — never in SQL against {@code NOW()}, which would silently inherit the
 * database session's timezone (all columns are {@code datetime(6)} with no zone).</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_verification_token")
public class VerificationToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "userId", nullable = false)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, length = 32)
  private TokenPurpose purpose;

  @Column(name = "tokenHash", nullable = false, length = 64, unique = true)
  private String tokenHash;

  @Column(name = "createdAt", nullable = false)
  private Instant createdAt;

  @Column(name = "expiresAt", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumedAt")
  private Instant consumedAt;

  @Column(name = "supersededAt")
  private Instant supersededAt;

  protected VerificationToken() {
  }

  private VerificationToken(String userId, TokenPurpose purpose, String tokenHash,
      Instant createdAt, Instant expiresAt) {
    this.userId = requireNonNull(userId, "userId must not be null");
    this.purpose = requireNonNull(purpose, "purpose must not be null");
    this.tokenHash = requireNonNull(tokenHash, "tokenHash must not be null");
    this.createdAt = requireNonNull(createdAt, "createdAt must not be null");
    this.expiresAt = requireNonNull(expiresAt, "expiresAt must not be null");
  }

  /**
   * Issues a new token.
   *
   * @param userId    the account the token belongs to
   * @param purpose   the flow the token authorises
   * @param tokenHash lower-case SHA-256 hex digest of the raw token
   * @param issuedAt  the issuing instant
   * @param lifetime  how long the token stays usable; must be positive
   * @return the new token, unconsumed and not superseded
   */
  public static VerificationToken issue(String userId, TokenPurpose purpose, String tokenHash,
      Instant issuedAt, Duration lifetime) {
    requireNonNull(issuedAt, "issuedAt must not be null");
    requireNonNull(lifetime, "lifetime must not be null");
    if (lifetime.isZero() || lifetime.isNegative()) {
      throw new IllegalArgumentException(
          "token lifetime must be positive, but was " + lifetime);
    }
    requireHexHash(tokenHash);
    return new VerificationToken(userId, purpose, tokenHash, issuedAt, issuedAt.plus(lifetime));
  }

  /**
   * Rejects anything that is not a lower-case SHA-256 hex digest.
   *
   * <p>Guards the column contract: {@code char(64)} holds a fixed-width hex digest, and a caller
   * passing a raw token or a differently-sized digest would fail silently against the unique
   * index instead of loudly here.</p>
   */
  private static void requireHexHash(String tokenHash) {
    requireNonNull(tokenHash, "tokenHash must not be null");
    if (!tokenHash.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException(
          "tokenHash must be a lower-case SHA-256 hex digest (64 characters), but was: "
              + tokenHash.length() + " characters");
    }
  }

  public Long id() {
    return id;
  }

  public String userId() {
    return userId;
  }

  public TokenPurpose purpose() {
    return purpose;
  }

  public String tokenHash() {
    return tokenHash;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public Instant consumedAt() {
    return consumedAt;
  }

  public Instant supersededAt() {
    return supersededAt;
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }

  public boolean isSuperseded() {
    return supersededAt != null;
  }

  /**
   * @param now the instant to evaluate against
   * @return {@code true} if the token has reached or passed its expiry
   */
  public boolean isExpiredAt(Instant now) {
    requireNonNull(now, "now must not be null");
    return !now.isBefore(expiresAt);
  }

  /**
   * The single predicate every consumption path must go through.
   *
   * @param now the instant to evaluate against
   * @return {@code true} only if the token is unconsumed, not superseded and not expired
   */
  public boolean isUsableAt(Instant now) {
    return !isConsumed() && !isSuperseded() && !isExpiredAt(now);
  }

  /**
   * Evaluates every rule that gates consumption, without mutating anything.
   *
   * <p>The single source of truth for the order of checks. The persistence adapter calls this inside
   * the transaction, so the rules are never re-implemented in SQL or in the adapter.</p>
   *
   * @param purpose the flow attempting consumption
   * @param now     the instant of consumption
   * @return empty if the token may be consumed, otherwise why not
   */
  public java.util.Optional<ConsumeFailure> checkConsumable(TokenPurpose purpose, Instant now) {
    requireNonNull(purpose, "purpose must not be null");
    requireNonNull(now, "now must not be null");
    if (this.purpose != purpose) {
      return java.util.Optional.of(ConsumeFailure.WRONG_PURPOSE);
    }
    if (isConsumed()) {
      return java.util.Optional.of(ConsumeFailure.ALREADY_CONSUMED);
    }
    if (isSuperseded()) {
      return java.util.Optional.of(ConsumeFailure.SUPERSEDED);
    }
    if (isExpiredAt(now)) {
      return java.util.Optional.of(ConsumeFailure.EXPIRED);
    }
    return java.util.Optional.empty();
  }

  /**
   * Marks this token as used.
   *
   * @param now the instant of consumption
   * @throws IllegalStateException if the token is no longer usable at {@code now}
   */
  public void consume(Instant now) {
    requireNonNull(now, "now must not be null");
    if (!isUsableAt(now)) {
      throw new IllegalStateException(
          "token is not usable at %s (consumed=%s, superseded=%s, expired=%s)"
              .formatted(now, isConsumed(), isSuperseded(), isExpiredAt(now)));
    }
    this.consumedAt = now;
  }

  /**
   * Marks this token as replaced by a newly issued token for the same (user, purpose).
   *
   * <p>Idempotent: superseding twice keeps the first timestamp, so the audit trail is not
   * rewritten by a retry.</p>
   *
   * @param now the instant of supersession
   */
  public void supersede(Instant now) {
    requireNonNull(now, "now must not be null");
    if (this.supersededAt == null) {
      this.supersededAt = now;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VerificationToken that = (VerificationToken) o;
    return Objects.equals(tokenHash, that.tokenHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokenHash);
  }
}
