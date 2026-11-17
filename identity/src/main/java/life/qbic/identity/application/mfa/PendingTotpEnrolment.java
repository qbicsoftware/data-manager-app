package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

/**
 * <b>A TOTP enrolment that has been started but not yet confirmed</b>
 *
 * <p>Carries everything the confirmation step needs — the account, the label, the <em>encrypted</em>
 * seed and the provisioning URI — and deliberately <b>no plaintext seed</b>. That is what makes it
 * safe to keep between the two steps in an HTTP session: a database or session dump yields ciphertext,
 * which is exactly what the factor row would hold anyway (AUTH-NFR-01).</p>
 *
 * <p>The URI is the exception and must be treated as sensitive: it contains the seed so the
 * authenticator can scan it. Render it once, never log it, never send it again after the factor is
 * confirmed.</p>
 *
 * @param userId         the account enrolling
 * @param displayName    the label the user chose, e.g. "iPhone (1Password)"
 * @param protectedSeed  Base64 AES-GCM ciphertext of the seed
 * @param provisioningUri the {@code otpauth://} URI for the QR code
 * @param createdAt      when the enrolment started; the window to confirm is bounded
 * @since 1.0.0
 */
public record PendingTotpEnrolment(String userId, String displayName, String protectedSeed,
                                   String provisioningUri, Instant createdAt) {

  public PendingTotpEnrolment {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(displayName, "displayName must not be null");
    requireNonNull(protectedSeed, "protectedSeed must not be null");
    requireNonNull(provisioningUri, "provisioningUri must not be null");
    requireNonNull(createdAt, "createdAt must not be null");
    if (userId.isBlank() || displayName.isBlank() || protectedSeed.isBlank()
        || provisioningUri.isBlank()) {
      throw new IllegalArgumentException(
          "a pending enrolment needs an account, a label, a protected seed and a provisioning URI");
    }
  }

  /**
   * Deliberately prints no material: this object may appear in a session dump or an exception.
   */
  @Override
  public String toString() {
    return "PendingTotpEnrolment[userId=" + userId + ", displayName=" + displayName
        + ", createdAt=" + createdAt + "]";
  }
}
