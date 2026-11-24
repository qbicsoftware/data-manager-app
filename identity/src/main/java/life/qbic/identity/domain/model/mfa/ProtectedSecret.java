package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Base64;

/**
 * <b>A second-factor secret that is already protected</b>
 *
 * <p>A TOTP seed must be <em>recovered</em> to compute the expected code, so it cannot be hashed the
 * way a personal access token is (ADR 0011 F2). It is therefore encrypted with AES-256-GCM under a
 * master key that lives in the PKCS12 vault, and this type is the only thing the domain ever sees:
 * the encoded blob, never the seed.</p>
 *
 * <p><b>Why this is a type and not a {@code String}.</b> The domain cannot encrypt — that is the
 * composition root's job (ADR-0002 D1: plaintext never escapes the infrastructure boundary). What the
 * domain <em>can</em> do is refuse to persist something that is not an encrypted blob, which closes
 * the cheapest way to violate AUTH-NFR-01: passing the raw seed through because the encryptor was not
 * wired yet. A raw base32 seed (16–64 characters) fails the length check below; a Base64-encoded
 * plaintext seed of the usual size decodes to far fewer than the 28 bytes that a 12-byte nonce plus a
 * 16-byte GCM tag require.</p>
 *
 * <p>The check is a shape check, not a proof of confidentiality: it cannot tell a well-formed
 * ciphertext from random bytes. It exists so that "not encrypted at all" fails loudly and early.</p>
 *
 * @since 1.0.0
 */
public final class ProtectedSecret {

  /** AES-GCM nonce length used by the application's encryptor (RFC 5116 §3.1). */
  static final int GCM_NONCE_BYTES = 12;
  /** GCM authentication tag length in bytes; Java's {@code Cipher} appends it to the ciphertext. */
  static final int GCM_TAG_BYTES = 16;
  /** Smallest possible blob: nonce, an empty ciphertext block and the tag. */
  static final int MIN_ENCODED_BYTES = GCM_NONCE_BYTES + GCM_TAG_BYTES;

  private final String encoded;

  private ProtectedSecret(String encoded) {
    this.encoded = encoded;
  }

  /**
   * Wraps an already-encrypted secret.
   *
   * @param base64Ciphertext {@code Base64(nonce || ciphertext || tag)} as produced by the
   *     second-factor encryptor
   * @return the validated secret
   * @throws IllegalArgumentException if the value is not Base64 or is too short to contain a nonce
   *     and an authentication tag — which is what a raw or merely encoded seed looks like
   */
  public static ProtectedSecret ofBase64(String base64Ciphertext) {
    requireNonNull(base64Ciphertext, "base64Ciphertext must not be null");
    String trimmed = base64Ciphertext.strip();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("a protected secret must not be blank");
    }
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(trimmed);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "a protected secret must be Base64-encoded ciphertext, but the value is not valid Base64",
          e);
    }
    if (decoded.length < MIN_ENCODED_BYTES) {
      throw new IllegalArgumentException(
          "a protected secret must hold a nonce and a GCM tag (at least " + MIN_ENCODED_BYTES
              + " bytes), but decoded to " + decoded.length
              + " bytes — a seed that was not encrypted looks exactly like this");
    }
    // Neither the bytes nor the encoding are secret material that must be zeroed here: the
    // plaintext never existed in this object, and the ciphertext is stored in the database anyway.
    Arrays.fill(decoded, (byte) 0);
    return new ProtectedSecret(trimmed);
  }

  /**
   * @return the encoded ciphertext, ready to be stored in {@code user_second_factor.secretCiphertext}
   */
  public String encoded() {
    return encoded;
  }

  /**
   * Deliberately reveals no material: this value ends up in logs and exception messages, and while
   * the ciphertext is not the seed, printing it invites exactly the handling this type prevents.
   */
  @Override
  public String toString() {
    return "ProtectedSecret[redacted]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof ProtectedSecret other && encoded.equals(other.encoded);
  }

  @Override
  public int hashCode() {
    return encoded.hashCode();
  }
}
