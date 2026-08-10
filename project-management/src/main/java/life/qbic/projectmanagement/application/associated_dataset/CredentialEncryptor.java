package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Encrypts and decrypts external provider user tokens.
 *
 * <p>Application-layer port (SPI): implemented by the infrastructure
 * layer. The interface is provider-agnostic — the same encryptor
 * handles tokens for InvenioRDM, LIMS, or any future external source,
 * all sharing the same master key.</p>
 *
 * <p>The infrastructure implementation (AES-256-GCM) uses a dedicated
 * master key from the PKCS12 vault (ADR-0002 S2).</p>
 *
 * <p>Output format: opaque encrypted blob (nonce + ciphertext + auth tag).
 * The application layer treats this as an opaque byte array — it never
 * inspects or interprets the contents.</p>
 *
 * @since 1.12.0
 */
public interface CredentialEncryptor {

  /**
   * Encrypts a plaintext token.
   *
   * @param plaintext the plaintext token as {@code char[]}
   * @return the encrypted blob (nonce + ciphertext + GCM tag)
   */
  byte[] encrypt(char[] plaintext);

  /**
   * Decrypts an encrypted token.
   *
   * @param encrypted the encrypted blob
   * @return the plaintext token as {@code char[]} — caller <strong>MUST</strong>
   *         zero after use ({@code Arrays.fill(result, '\0')} in a
   *         finally block)
   */
  char[] decrypt(byte[] encrypted);

}
