package life.qbic.identity.application.mfa;

/**
 * <b>Protects second-factor secrets at rest</b>
 *
 * <p>Application-layer port (SPI) for the one class of factor material that must be <em>recoverable</em>:
 * the TOTP seed. It cannot be hashed the way a personal access token is (ADR 0011 F2), so it is
 * encrypted with a master key that lives in the PKCS12 vault under a dedicated alias, and only the
 * encoded ciphertext reaches the database (AUTH-NFR-01).</p>
 *
 * <p><b>Output format:</b> {@code Base64(nonce ‖ ciphertext ‖ GCM tag)}, which is what
 * {@code ProtectedSecret} validates before it will let a factor be persisted. The application layer
 * treats the value as opaque; it never inspects it.</p>
 *
 * <p><b>Why not the personal-access-token encoder:</b> that one is a one-way PBKDF2 hash. Verifying a
 * TOTP code requires the seed back, so a hash would destroy the factor rather than protect it.</p>
 *
 * @since 1.0.0
 */
public interface SecondFactorSecretProtector {

  /**
   * Encrypts a seed.
   *
   * @param plaintext the seed as {@code char[]}; the caller keeps ownership and must zero it after
   *     the call ({@code Arrays.fill(plaintext, '\0')} in a {@code finally} block)
   * @return the Base64-encoded ciphertext
   */
  String protect(char[] plaintext);

  /**
   * Decrypts a seed.
   *
   * @param protectedSecret the Base64-encoded ciphertext as stored
   * @return the seed as {@code char[]}; the caller <b>must</b> zero it after use
   * @throws SecondFactorSecretUnavailableException if the stored value is not decryptable — the
   *     master key is missing or different, or the ciphertext is corrupted. Never fail open: a
   *     factor that cannot be decrypted must be treated as unavailable, not as valid
   *     (AUTH-NFR-02)
   */
  char[] reveal(String protectedSecret);
}
