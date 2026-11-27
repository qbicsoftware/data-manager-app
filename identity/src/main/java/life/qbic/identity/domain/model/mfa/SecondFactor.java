package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;

/**
 * <b>Second factor</b>
 *
 * <p>One enrolled credential of one account: either a TOTP shared secret or a WebAuthn public
 * credential. Rows are never hard-deleted — removing a factor sets {@link #disabledAt()}, so the
 * audit trail (AUTH-R-04) keeps showing that the factor existed and when it stopped being usable.</p>
 *
 * <p><b>What this aggregate deliberately does not do:</b> it does not encrypt, decrypt, validate a
 * one-time code, or decide whether a user must present a factor. Encryption belongs to the
 * composition root (ADR-0002 D1), code validation belongs to the TOTP service that has the seed, and
 * the policy decision belongs to {@link MfaGate}.</p>
 *
 * <p><b>Invariants enforced here</b>, because they are the cheapest place to stop a broken factor
 * from ever reaching the database:</p>
 * <ul>
 *   <li>a factor is either a protected TOTP secret <em>or</em> a WebAuthn credential, never both and
 *       never neither — the same shape {@code chk_user_second_factor_shape} enforces in DDL;</li>
 *   <li>a TOTP secret must be an encrypted blob ({@link ProtectedSecret}), not a seed that merely
 *       looks encoded;</li>
 *   <li>a disabled factor cannot be used, and re-enabling is not expressible: the fix for a lost
 *       device is a new enrolment, not a resurrected old credential.</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_second_factor")
public class SecondFactor {

  /**
   * Matches {@code user_second_factor.displayName varchar(255)}.
   *
   * <p>Public because the enrolment dialog has to cap its input at the same number: a form that allows
   * more than the column holds would fail on submit, after the user has already scanned the QR code.</p>
   */
  public static final int MAX_DISPLAY_NAME_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "userId", nullable = false)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 16)
  private SecondFactorType type;

  @Column(name = "secretCiphertext", columnDefinition = "text")
  private String secretCiphertext;

  @Column(name = "webauthnCredential", columnDefinition = "text")
  private String webauthnCredential;

  @Column(name = "displayName", nullable = false)
  private String displayName;

  @Column(name = "createdAt", nullable = false)
  private Instant createdAt;

  @Column(name = "lastUsedAt")
  private Instant lastUsedAt;

  /**
   * The highest TOTP time step this factor has ever accepted ({@code null} until the first login).
   *
   * <p><b>Replay protection (MFA-20).</b> A code is valid for its 30-second step, and verification
   * tolerates one step either side, so an already-seen code stays usable for ninety seconds — and any
   * number of times within it. The counter is fed from the RFC 6238 step number, so this value along
   * with the rule {@code acceptedStep > lastAcceptedStep} makes every used code unusable for good.</p>
   *
   * <p>It is a monotonic counter with no expiry: no sweeper, no window to clean up, and it cannot be
   * rolled back by a clock change on the client. {@code datetime(6)} with no zone would have inherited
   * the database session's timezone had this been a timestamp — a step number sidesteps that.</p>
   */
  @Column(name = "lastAcceptedStep")
  private Long lastAcceptedStep;

  @Column(name = "disabledAt")
  private Instant disabledAt;

  protected SecondFactor() {
    // for JPA
  }

  private SecondFactor(String userId, SecondFactorType type, String secretCiphertext,
      String webauthnCredential, String displayName, Instant createdAt) {
    this.userId = requireNonNull(userId, "userId must not be null");
    if (userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    this.type = requireNonNull(type, "type must not be null");
    this.secretCiphertext = secretCiphertext;
    this.webauthnCredential = webauthnCredential;
    this.displayName = requireDisplayName(displayName);
    this.createdAt = requireNonNull(createdAt, "createdAt must not be null");
  }

  /**
   * Enrols a TOTP factor.
   *
   * @param userId      the account
   * @param secret      the seed, already encrypted with the second-factor master key
   * @param displayName a user-facing label, e.g. "iPhone (1Password)"
   * @param enrolledAt  when the enrolment completed
   * @return the new, active factor
   */
  public static SecondFactor enrolTotp(String userId, ProtectedSecret secret, String displayName,
      Instant enrolledAt) {
    requireNonNull(secret, "secret must not be null");
    return new SecondFactor(userId, SecondFactorType.TOTP, secret.encoded(), null, displayName,
        enrolledAt);
  }

  /**
   * Enrols a WebAuthn factor.
   *
   * @param userId             the account
   * @param webauthnCredential serialised public credential (credential id, public key, counter,
   *     transports) — never a secret
   * @param displayName        a user-facing label, e.g. "YubiKey 5 (USB-C)"
   * @param enrolledAt         when the enrolment completed
   * @return the new, active factor
   */
  public static SecondFactor enrolWebAuthn(String userId, String webauthnCredential,
      String displayName, Instant enrolledAt) {
    requireNonNull(webauthnCredential, "webauthnCredential must not be null");
    if (webauthnCredential.isBlank()) {
      throw new IllegalArgumentException(
          "webauthnCredential must not be blank: a WebAuthn factor without a credential id and "
              + "public key cannot be verified");
    }
    return new SecondFactor(userId, SecondFactorType.WEBAUTHN, null, webauthnCredential,
        displayName, enrolledAt);
  }

  private static String requireDisplayName(String displayName) {
    requireNonNull(displayName, "displayName must not be null");
    String trimmed = displayName.strip();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "displayName must not exceed " + MAX_DISPLAY_NAME_LENGTH + " characters, but was "
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

  public SecondFactorType type() {
    return type;
  }

  /**
   * @return the encrypted seed, present only for {@link SecondFactorType#TOTP}
   */
  public Optional<ProtectedSecret> secret() {
    return Optional.ofNullable(secretCiphertext).map(ProtectedSecret::ofBase64);
  }

  /**
   * @return the serialised public credential, present only for {@link SecondFactorType#WEBAUTHN}
   */
  public Optional<String> webauthnCredential() {
    return Optional.ofNullable(webauthnCredential);
  }

  public String displayName() {
    return displayName;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant lastUsedAt() {
    return lastUsedAt;
  }

  public Instant disabledAt() {
    return disabledAt;
  }

  /**
   * @return {@code true} while the factor may be presented and counted as enrolment (AUTH-R-02)
   */
  public boolean isActive() {
    return disabledAt == null;
  }

  /**
   * Relabels the factor (AUTH-R-02 requires renaming without re-enrolling).
   *
   * @param newDisplayName the new user-facing label
   */
  public void rename(String newDisplayName) {
    this.displayName = requireDisplayName(newDisplayName);
  }

  /**
   * Removes the factor. Idempotent: a second call keeps the first timestamp, so the audit trail
   * records when the user actually removed it, not when the code last ran.
   *
   * @param now the removal instant
   * @return {@code true} if this call disabled the factor
   */
  public boolean disable(Instant now) {
    requireNonNull(now, "now must not be null");
    if (!isActive()) {
      return false;
    }
    this.disabledAt = now;
    return true;
  }

  /**
   * Records a successful verification.
   *
   * @param now the instant the factor was used
   * @throws IllegalStateException if the factor is disabled — a removed credential must never
   *     authenticate, and failing loudly here is what stops a stale row in a session from doing so
   */
  public void markUsed(Instant now) {
    requireNonNull(now, "now must not be null");
    if (!isActive()) {
      throw new IllegalStateException(
          "a disabled second factor cannot be used (factor " + id + " of user " + userId + ")");
    }
    this.lastUsedAt = now;
  }

  /**
   * @return the highest time step this factor has accepted, or empty if it has never been used
   */
  public java.util.OptionalLong lastAcceptedStep() {
    return lastAcceptedStep == null
        ? java.util.OptionalLong.empty()
        : java.util.OptionalLong.of(lastAcceptedStep);
  }


  /**
   * Whether a code from this step may be accepted, i.e. it is strictly newer than anything used so far.
   *
   * <p>Strictly greater, not "different": the tolerance window means steps {@code n-1}, {@code n} and
   * {@code n+1} all match the same code, so '<i>not already used</i>' would let an attacker replay
   * step {@code n-1} after a legitimate login at {@code n}.</p>
   *
   * <p>The cost, stated plainly: two logins inside one 30-second step cannot both succeed, because
   * both carry the same code and the second is a replay by definition. A user logging in twice in
   * quick succession waits at most 30 seconds for the next code.</p>
   *
   * @param step the step a presented code matched
   * @return {@code true} if this step is newer than every step accepted so far
   */
  public boolean acceptsStep(long step) {
    return lastAcceptedStep == null || step > lastAcceptedStep;
  }

  /**
   * Records that a code from this step was accepted, so it — and everything older — cannot be used
   * again.
   *
   * @param step the matched time step; must be strictly newer than the current mark
   * @throws IllegalArgumentException if the step would move the mark backwards
   */
  public void markAcceptedStep(long step) {
    if (!acceptsStep(step)) {
      throw new IllegalArgumentException(
          "refusing to move the accepted-step mark backwards: step " + step
              + " is not newer than " + lastAcceptedStep);
    }
    this.lastAcceptedStep = step;
  }
}
