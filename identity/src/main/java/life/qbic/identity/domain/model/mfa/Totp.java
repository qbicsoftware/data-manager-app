package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * <b>TOTP — RFC 6238 over RFC 4226 (HOTP)</b>
 *
 * <p>The universal second factor of this milestone (AUTH-R-02): it needs nothing but a smartphone,
 * so the requirement never turns into a device-procurement programme. This class is the whole
 * algorithm — seed generation, code derivation and verification — kept dependency-free and pure so
 * that the security-relevant part has no framework, no clock and no database in it.</p>
 *
 * <h3>Fixed parameters, and why they are not configurable</h3>
 * <ul>
 *   <li><b>SHA-1, 6 digits, 30-second period.</b> The RFC 6238 default is SHA-1 with these
 *       parameters, and every authenticator app implements it. Offering SHA-256 or 8 digits would
 *       add a compatibility matrix for no security gain: the seed is 160 bits, so the strength comes
 *       from the key, not the digest, and 6 digits in a 30-second window is already the rate-limited
 *       case (an online attacker gets one guess per window, and M5-4 throttles it).</li>
 *   <li><b>±1 period skew.</b> The user's phone clock drifts, and a device that is 20 seconds out
 *       would otherwise fail permanently and look like a lost phone. One step either side is the
 *       usual compromise; wider is not offered, because each extra step widens the window in which a
 *       relayed code stays usable.</li>
 *   <li><b>20-byte seed.</b> The RFC 4226 recommendation; 160 bits of entropy, encoded as 32 Base32
 *       characters, which is the shortest form that fits every authenticator's manual-entry field.</li>
 * </ul>
 *
 * <p><b>Verification is constant-time.</b> Codes are compared with
 * {@link MessageDigest#isEqual}, not {@code String.equals}, so the comparison does not leak how many
 * leading digits were right. Against six digits the timing channel is theoretical, but it costs
 * nothing to close and this is the one place where such a habit matters.</p>
 *
 * <p><b>What this class does not do:</b> replay protection. A code stays valid for its whole step,
 * so a code used once is technically usable again within the same 30 seconds. That is a property of
 * the login step (M4), which must refuse to accept a step it has already accepted for the same
 * account — not of the algorithm.</p>
 *
 * @since 1.0.0
 */
public final class Totp {

  /** Digits in a generated code. */
  public static final int DIGITS = 6;
  /** Length of one time step. */
  public static final int PERIOD_SECONDS = 30;
  /** How many steps of clock drift are tolerated on either side. */
  public static final int ALLOWED_STEP_SKEW = 1;
  /** Seed entropy in bytes (RFC 4226 §4.1 recommendation). */
  static final int SEED_BYTES = 20;

  private static final String HMAC_ALGORITHM = "HmacSHA1";
  private static final int MODULUS = 1_000_000;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private Totp() {
  }

  /**
   * @return a fresh seed as 32 upper-case Base32 characters, never stored unencrypted
   */
  public static String newSeed() {
    byte[] seed = new byte[SEED_BYTES];
    SECURE_RANDOM.nextBytes(seed);
    return Base32.encode(seed);
  }

  /**
   * Derives the code for the step that contains {@code at}.
   *
   * @param base32Seed the shared seed
   * @param at         the instant to derive the code for
   * @return six digits, zero-padded
   */
  public static String code(String base32Seed, Instant at) {
    requireNonNull(at, "at must not be null");
    return codeForStep(base32Seed, stepAt(at));
  }

  /**
   * Verifies a presented code against the current step and one step either side.
   *
   * @param base32Seed the shared seed
   * @param presented  the code the user typed; surrounding whitespace is ignored
   * @param at         the instant of verification
   * @return {@code true} only if the code matches a tolerated step and is well-formed
   */
  public static boolean verify(String base32Seed, String presented, Instant at) {
    return acceptedStep(base32Seed, presented, at).isPresent();
  }

  /**
   * Verifies a presented code and reports <b>which time step it matched</b>.
   *
   * <p>The step is the identity of the 30-second window that produced the code: the value fed into the
   * HMAC counter, not a proxy for it. Callers need it for replay protection — "this code is valid" is
   * not enough to tell a fresh code from one that was accepted a minute ago, because the tolerance
   * window makes the same code match three consecutive steps.</p>
   *
   * <p>Recording the <em>matched</em> step rather than the current one matters: a phone whose clock is
   * behind matches the previous step, and recording the current step would then permanently block that
   * phone's next code — derived from a step it never used.</p>
   *
   * @param base32Seed the shared seed
   * @param presented  the code the user typed; surrounding whitespace is ignored
   * @param at         the instant of verification
   * @return the matched step, or empty when the code is malformed, wrong, or outside the tolerated skew
   */
  public static java.util.OptionalLong acceptedStep(String base32Seed, String presented,
      Instant at) {
    requireNonNull(presented, "presented must not be null");
    requireNonNull(at, "at must not be null");
    String candidate = presented.strip();
    if (!candidate.matches("\\d{" + DIGITS + "}")) {
      return java.util.OptionalLong.empty();
    }
    long step = stepAt(at);
    for (int offset = -ALLOWED_STEP_SKEW; offset <= ALLOWED_STEP_SKEW; offset++) {
      long candidateStep = step + offset;
      if (candidateStep < 0) {
        continue;
      }
      byte[] expected = codeForStep(base32Seed, candidateStep).getBytes(StandardCharsets.US_ASCII);
      if (MessageDigest.isEqual(expected, candidate.getBytes(StandardCharsets.US_ASCII))) {
        return java.util.OptionalLong.of(candidateStep);
      }
    }
    return java.util.OptionalLong.empty();
  }

  /**
   * @param at an instant
   * @return the RFC 6238 time step (Unix time divided by the period)
   */
  static long stepAt(Instant at) {
    return Math.floorDiv(at.getEpochSecond(), (long) PERIOD_SECONDS);
  }

  private static String codeForStep(String base32Seed, long step) {
    byte[] seed = Base32.decode(base32Seed);
    if (seed.length == 0) {
      throw new IllegalArgumentException("the TOTP seed must not be empty");
    }
    byte[] counter = ByteBuffer.allocate(Long.BYTES).putLong(step).array();
    byte[] digest;
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(seed, HMAC_ALGORITHM));
      digest = mac.doFinal(counter);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA1 is not available in this JVM", e);
    }
    // RFC 4226 §5.4 dynamic truncation: the low nibble of the last byte selects a 4-byte window.
    int offset = digest[digest.length - 1] & 0x0F;
    int binary = ((digest[offset] & 0x7F) << 24)
        | ((digest[offset + 1] & 0xFF) << 16)
        | ((digest[offset + 2] & 0xFF) << 8)
        | (digest[offset + 3] & 0xFF);
    return String.format(Locale.ROOT, "%0" + DIGITS + "d", binary % MODULUS);
  }
}
