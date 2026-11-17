package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

/**
 * <b>Base32 (RFC 4648) for TOTP seeds</b>
 *
 * <p>Authenticator applications exchange seeds as Base32 — the format every QR code carries and
 * every manual-entry field accepts. The JDK has no Base32 codec, and pulling a library in for 40
 * lines would mean a new dependency behind a signature check (milestone §6) for no benefit.</p>
 *
 * <p><b>Deliberate deviations from a strict codec</b>, both on the decoding side only:</p>
 * <ul>
 *   <li>lower case is accepted, because users type seeds by hand from a printed page;</li>
 *   <li>trailing {@code =} padding is accepted and ignored, because some authenticator exports keep
 *       it; encoding never emits it.</li>
 * </ul>
 * <p>Everything else is rejected: an unknown character, an impossible length, or padding in the
 * middle is a seed that will not verify, and failing loudly at parse time is cheaper than a support
 * ticket about "the code is always wrong".</p>
 *
 * @since 1.0.0
 */
public final class Base32 {

  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final int[] LOOKUP = new int[128];

  static {
    java.util.Arrays.fill(LOOKUP, -1);
    for (int i = 0; i < ALPHABET.length(); i++) {
      char upper = ALPHABET.charAt(i);
      LOOKUP[upper] = i;
      LOOKUP[Character.toLowerCase(upper)] = i;
    }
  }

  private Base32() {
  }

  /**
   * @param bytes the bytes to encode
   * @return the upper-case, unpadded Base32 representation
   */
  public static String encode(byte[] bytes) {
    requireNonNull(bytes, "bytes must not be null");
    if (bytes.length == 0) {
      return "";
    }
    StringBuilder out = new StringBuilder((bytes.length * 8 + 4) / 5);
    int buffer = 0;
    int bitsInBuffer = 0;
    for (byte b : bytes) {
      buffer = (buffer << 8) | (b & 0xFF);
      bitsInBuffer += 8;
      while (bitsInBuffer >= 5) {
        bitsInBuffer -= 5;
        out.append(ALPHABET.charAt((buffer >>> bitsInBuffer) & 0x1F));
      }
    }
    if (bitsInBuffer > 0) {
      out.append(ALPHABET.charAt((buffer << (5 - bitsInBuffer)) & 0x1F));
    }
    return out.toString();
  }

  /**
   * @param encoded the Base32 text, optionally lower-case and/or padded
   * @return the decoded bytes
   * @throws IllegalArgumentException if the text contains a character outside the alphabet or
   *     cannot be produced by encoding whole bytes
   */
  public static byte[] decode(String encoded) {
    requireNonNull(encoded, "encoded must not be null");
    String trimmed = encoded.strip();
    int end = trimmed.length();
    while (end > 0 && trimmed.charAt(end - 1) == '=') {
      end--;
    }
    if (end == 0) {
      return new byte[0];
    }
    // 1, 3 and 6 Base32 characters cannot describe whole bytes: they carry 5, 15 and 30 bits.
    int remainder = end % 8;
    if (remainder == 1 || remainder == 3 || remainder == 6) {
      throw new IllegalArgumentException(
          "not a valid Base32 length: " + end + " characters carry a partial byte");
    }
    byte[] out = new byte[end * 5 / 8];
    int buffer = 0;
    int bitsInBuffer = 0;
    int index = 0;
    for (int i = 0; i < end; i++) {
      char c = trimmed.charAt(i);
      int value = c < 128 ? LOOKUP[c] : -1;
      if (value < 0) {
        throw new IllegalArgumentException(
            "invalid Base32 character at position " + i + " (only A-Z and 2-7 are allowed)");
      }
      buffer = (buffer << 5) | value;
      bitsInBuffer += 5;
      if (bitsInBuffer >= 8) {
        bitsInBuffer -= 8;
        out[index++] = (byte) ((buffer >>> bitsInBuffer) & 0xFF);
      }
    }
    return out;
  }
}
