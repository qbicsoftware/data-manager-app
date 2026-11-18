package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>Hashes recovery codes so they can be looked up without knowing the account</b>
 *
 * <p>A recovery code is presented by a user who cannot log in, so there is no account id to scope the
 * query with: the code <em>is</em> the lookup key. That rules out a per-row salted hash (you cannot
 * hash a guess without knowing which salt to use) and it is why this is a <b>keyed</b> hash:
 * {@code HMAC-SHA-256(pepper, normalised code)} is deterministic, indexable and — unlike a plain
 * SHA-256 — not verifiable from a stolen database alone.</p>
 *
 * <h3>Why not PBKDF2, although the PAT encoder uses it</h3>
 * <p>Password stretching exists to make <em>low-entropy, user-chosen</em> secrets expensive to guess.
 * A recovery code is 60 bits of {@link java.security.SecureRandom} from a 32-character alphabet; there
 * is nothing to stretch. Iterations would add ~100 ms to every redemption and buy nothing, and a
 * per-code salt would break the one property the login path needs: lookup by hash. The pepper is what
 * provides the "database dump is not enough" property here.</p>
 *
 * <h3>The pepper, and the vault-key rule</h3>
 * <p>ADR 0011 B1 requires recovery codes to keep working when the PKCS12 vault key is lost — that is
 * the whole point of having them outside the encrypted-seed path (milestone risk R8). The pepper is
 * therefore deployment configuration, like the personal-access-token salt, and <b>not</b> the vault
 * master key. Losing it invalidates every recovery code, but not the codes' role in the design: an
 * administrator can still re-enrol a user (M6-1). Back it up with the application configuration.</p>
 *
 * <p>The output is a lower-case hex digest, 64 characters, matching
 * {@code user_recovery_code.codeHash}.</p>
 *
 * @since 1.0.0
 */
@Service
public class RecoveryCodeEncoder {

  /** A pepper shorter than this is guessable from a single leaked hash; fail at construction. */
  static final int MIN_PEPPER_LENGTH = 32;

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final byte[] pepper;

  /**
   * @param pepper the deployment secret; must be at least {@value #MIN_PEPPER_LENGTH} characters and
   *     must not be the vault master key (ADR 0011 B1)
   */
  public RecoveryCodeEncoder(@Value("${qbic.security.recovery-code.pepper}") String pepper) {
    requireNonNull(pepper, "pepper must not be null");
    if (pepper.strip().length() < MIN_PEPPER_LENGTH) {
      throw new IllegalArgumentException(
          "the recovery-code pepper must be at least " + MIN_PEPPER_LENGTH
              + " characters, otherwise a single leaked hash is guessable");
    }
    this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Hashes a code in whatever shape the user typed it.
   *
   * @param code the presented or freshly generated code
   * @return the lower-case HMAC-SHA-256 hex digest
   * @throws IllegalArgumentException if the input is not a recovery code
   */
  public String hash(String code) {
    String normalized = RecoveryCodes.normalize(code);
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(pepper, HMAC_ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.US_ASCII)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA-256 is not available in this JVM", e);
    }
  }

  /**
   * Re-verifies a code against a stored hash in constant time.
   *
   * <p>Not needed on the login path — that one looks the hash up in the database — but required
   * wherever a hash is compared in memory (tests, an administrative check), so the comparison never
   * leaks a prefix match.</p>
   *
   * @param presentedCode the code the user typed
   * @param storedHash    the hash from {@code user_recovery_code.codeHash}
   * @return {@code true} if the code matches
   */
  public boolean matches(String presentedCode, String storedHash) {
    requireNonNull(storedHash, "storedHash must not be null");
    String candidate;
    try {
      candidate = hash(presentedCode);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return MessageDigest.isEqual(
        candidate.getBytes(StandardCharsets.US_ASCII),
        storedHash.getBytes(StandardCharsets.US_ASCII));
  }

  /**
   * Deliberately prints nothing: this object holds the pepper.
   */
  @Override
  public String toString() {
    return "RecoveryCodeEncoder[pepper=redacted]";
  }
}
