package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * <b>Recovery code</b>
 *
 * <p>A single-use break-glass code (AUTH-R-03). Only the hash is stored: a database copy must not let
 * anyone reconstruct a code, and this is the one credential class that is deliberately <em>not</em>
 * protected by the vault master key (ADR 0011 B1) — if the key is lost, TOTP is gone for everyone,
 * and recovery codes must still work or the only way back in is an administrator.</p>
 *
 * <p>Three end-states, and they answer three different support questions:</p>
 * <ul>
 *   <li>{@link #usedAt()} — the code was spent (each code works once);</li>
 *   <li>{@link #invalidatedAt()} — a newer set was generated, retiring this one (M3-4);</li>
 *   <li>otherwise the code is usable until it is spent — recovery codes do not expire, because an
 *       expiry would recreate the lockout they exist to prevent.</li>
 * </ul>
 *
 * <p><b>Why no expiry.</b> A recovery code is the last self-service route past a lost device. A
 * deadline on it means a user who kept the printed sheet but did not need it for a year is locked
 * out by a calendar rather than by an event, which is the failure mode AUTH-R-03 rules out. They are
 * invalidated by regeneration instead, which the user controls.</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_recovery_code")
public class RecoveryCode {

  /** Matches {@code user_recovery_code.codeHash varchar(255)}. */
  static final int MAX_HASH_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "userId", nullable = false)
  private String userId;

  @Column(name = "codeHash", nullable = false, unique = true)
  private String codeHash;

  @Column(name = "createdAt", nullable = false)
  private Instant createdAt;

  @Column(name = "usedAt")
  private Instant usedAt;

  @Column(name = "invalidatedAt")
  private Instant invalidatedAt;

  protected RecoveryCode() {
    // for JPA
  }

  private RecoveryCode(String userId, String codeHash, Instant createdAt) {
    this.userId = requireNonNull(userId, "userId must not be null");
    if (userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    this.codeHash = requireHash(codeHash);
    this.createdAt = requireNonNull(createdAt, "createdAt must not be null");
  }

  /**
   * Creates one code of a freshly generated set.
   *
   * @param userId     the account the code belongs to
   * @param codeHash   hash of the raw code, produced by the recovery-code encoder
   * @param generatedAt when the set was generated
   * @return an unused, non-invalidated code
   */
  public static RecoveryCode issue(String userId, String codeHash, Instant generatedAt) {
    return new RecoveryCode(userId, codeHash, generatedAt);
  }

  private static String requireHash(String codeHash) {
    requireNonNull(codeHash, "codeHash must not be null");
    String trimmed = codeHash.strip();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(
          "codeHash must not be blank: a recovery code without its hash cannot be verified");
    }
    if (trimmed.length() > MAX_HASH_LENGTH) {
      throw new IllegalArgumentException(
          "codeHash must not exceed " + MAX_HASH_LENGTH + " characters, but was "
              + trimmed.length());
    }
    return trimmed;
  }

  public Long id() {
    return id;
  }

  public String userId() {
    return userId;
  }

  public String codeHash() {
    return codeHash;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant usedAt() {
    return usedAt;
  }

  public Instant invalidatedAt() {
    return invalidatedAt;
  }

  /**
   * @return {@code true} only while the code is neither spent nor retired by regeneration
   */
  public boolean isUsable() {
    return usedAt == null && invalidatedAt == null;
  }

  /**
   * Spends the code.
   *
   * <p>Must be called inside the same transaction that locked the row
   * ({@code RecoveryCodeRepository.consumeIfUsable}); without the lock two concurrent submissions of
   * the same code both read {@code usedAt = NULL} and both succeed, which turns one code into two.</p>
   *
   * @param now the consumption instant
   * @throws IllegalStateException if the code was already spent or retired
   */
  public void consume(Instant now) {
    requireNonNull(now, "now must not be null");
    if (!isUsable()) {
      throw new IllegalStateException(
          usedAt != null
              ? "recovery code was already used at " + usedAt
              : "recovery code was invalidated at " + invalidatedAt);
    }
    this.usedAt = now;
  }

  /**
   * Retires an unspent code because a new set was generated.
   *
   * <p>Spent codes keep their {@code usedAt} and are not re-marked: the audit trail should say "used",
   * not "invalidated", for a code that was actually used.</p>
   *
   * @param now the regeneration instant
   * @return {@code true} if this call retired the code
   */
  public boolean invalidate(Instant now) {
    requireNonNull(now, "now must not be null");
    if (!isUsable()) {
      return false;
    }
    this.invalidatedAt = now;
    return true;
  }
}
