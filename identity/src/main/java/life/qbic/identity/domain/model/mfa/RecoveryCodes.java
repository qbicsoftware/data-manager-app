package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Recovery-code format and generation (AUTH-R-03)</b>
 *
 * <p>Recovery codes are the self-service way past a lost device. Two properties decide whether they
 * are a safety net or a weakness: enough entropy that guessing is hopeless, and a shape a human can
 * still read off a printed sheet.</p>
 *
 * <ul>
 *   <li><b>12 characters from the 32-character Base32 alphabet = 60 bits.</b> At 10 codes per set that
 *       is 60 bits each, which no online attempt can reach (and M5-4 throttles the path anyway) and no
 *       offline attack can brute-force through a keyed hash. Longer would only make the sheet harder to
 *       type; shorter would start to matter.</li>
 *   <li><b>No digit/letter collisions.</b> The alphabet is RFC 4648 Base32 ({@code A–Z} plus
 *       {@code 2–7}), so the ambiguous digits {@code 0} and {@code 1} never appear at all, and the
 *       printed form is grouped {@code XXXX-XXXX-XXXX} — the hyphens are display only and are
 *       stripped on input. (It does not go as far as Crockford's alphabet, which also drops
 *       {@code I/L/O/U}; that would make the codec and the normalisation one more thing to get
 *       wrong, and the digits are where the real confusion is.)</li>
 * </ul>
 *
 * <p>Generation uses {@link SecureRandom}, never {@code Random} or a UUID: a predictable recovery code
 * is a permanent backdoor into the account, and UUID v4 is only 122 bits of the wrong shape anyway.</p>
 *
 * @since 1.0.0
 */
public final class RecoveryCodes {

  /** Codes issued per set (milestone M3-4). */
  public static final int SET_SIZE = 10;
  /** Random characters per code; 12 × 5 bits = 60 bits. */
  public static final int CODE_LENGTH = 12;

  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final int GROUP_SIZE = 4;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private RecoveryCodes() {
  }

  /**
   * @return a fresh set of {@link #SET_SIZE} distinct, unformatted codes
   */
  public static List<String> generate() {
    List<String> codes = new ArrayList<>(SET_SIZE);
    while (codes.size() < SET_SIZE) {
      String candidate = randomCode();
      // Distinctness is not a security property (60 bits make collisions absurd), but duplicate rows
      // in a printed set look like a bug to the user and cost one line of code to rule out.
      if (!codes.contains(candidate)) {
        codes.add(candidate);
      }
    }
    return List.copyOf(codes);
  }

  /**
   * @param code an unformatted or formatted code
   * @return the code as printed, grouped for readability: {@code XXXX-XXXX-XXXX}
   */
  public static String format(String code) {
    String normalized = normalize(code);
    StringBuilder out = new StringBuilder(CODE_LENGTH + CODE_LENGTH / GROUP_SIZE);
    for (int i = 0; i < normalized.length(); i++) {
      if (i > 0 && i % GROUP_SIZE == 0) {
        out.append('-');
      }
      out.append(normalized.charAt(i));
    }
    return out.toString();
  }

  /**
   * Brings user input into the canonical form: upper case, without spaces or hyphens.
   *
   * <p>This is the <em>only</em> normalisation in the system. It is called inside the encoder, so a
   * code cannot be hashed in one shape and typed in another — the failure mode would be a user with a
   * correct sheet and a code that "does not work".</p>
   *
   * @param input the code as the user typed it
   * @return the canonical 12-character code
   * @throws IllegalArgumentException if the input cannot be a code — wrong length or a character
   *     outside the alphabet. Callers on an authentication path must treat this as "invalid", never as
   *     an error to report verbatim (AUTH-NFR-03)
   */
  public static String normalize(String input) {
    requireNonNull(input, "input must not be null");
    String stripped = input.strip().toUpperCase(java.util.Locale.ROOT)
        .replace("-", "")
        .replace(" ", "");
    if (stripped.length() != CODE_LENGTH) {
      throw new IllegalArgumentException(
          "a recovery code is " + CODE_LENGTH + " characters, but the input has "
              + stripped.length());
    }
    for (int i = 0; i < stripped.length(); i++) {
      if (ALPHABET.indexOf(stripped.charAt(i)) < 0) {
        throw new IllegalArgumentException(
            "invalid recovery-code character at position " + i);
      }
    }
    return stripped;
  }

  private static String randomCode() {
    StringBuilder out = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      out.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
    }
    return out.toString();
  }
}
