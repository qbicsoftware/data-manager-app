package life.qbic.identity.domain.model.verification;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * <b>Verification token factory</b>
 *
 * <p>Generates raw verification tokens and derives the stored hash from them.</p>
 *
 * <h3>Why a plain SHA-256 and not the personal-access-token encoder</h3>
 * <p>{@code PersonalAccessTokenEncoder} derives its key from a single deployment-wide salt supplied
 * as the environment variable {@code qbic.access-token.salt}, and emits
 * {@code iterations:salt:hash}. Consumption of a verification token is a
 * {@code WHERE tokenHash = ?} lookup, so keying the digest on a rotatable environment variable would
 * silently invalidate every link in flight the moment that value changes.</p>
 *
 * <p>Password-based stretching is also the wrong tool here: PBKDF2 exists to make <em>low-entropy,
 * user-chosen</em> secrets expensive to guess. A verification token is 32 bytes of
 * {@link SecureRandom}, so there is nothing to stretch against — and at 100k iterations it would add
 * ~50–100 ms to every link consumption for no security benefit.</p>
 *
 * <p>What matters for a token hash is preimage resistance (SHA-256 is fine) and that lookups are
 * deterministic (they must be, to be indexable).</p>
 *
 * @since 1.0.0
 */
public final class VerificationTokens {

  /** Raw token entropy. 32 bytes = 256 bits, matching the bound in AUTH-R-05's rationale. */
  static final int TOKEN_BYTES = 32;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private VerificationTokens() {
  }

  /**
   * @return a fresh URL-safe raw token. Shown once, in the email, and never stored.
   */
  public static String newRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * Derives the stored hash of a raw token.
   *
   * @param rawToken the raw token from the link
   * @return the lower-case SHA-256 hex digest, 64 characters
   */
  public static String hash(String rawToken) {
    requireNonNull(rawToken, "rawToken must not be null");
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      // Every conforming JRE ships SHA-256; this is unreachable in practice.
      throw new IllegalStateException("SHA-256 is not available in this JVM", e);
    }
  }
}
