package life.qbic.projectmanagement.infrastructure.external;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import life.qbic.projectmanagement.application.associated_dataset.CredentialEncryptor;
import org.springframework.lang.NonNull;

/**
 * AES-256-GCM implementation of {@link CredentialEncryptor}.
 *
 * <p>Uses a master {@link SecretKey} loaded from the PKCS12 vault at
 * application startup (ADR-0002 S2). The key is resolved by the Spring
 * configuration ({@code InvenioRdmConfiguration}) which reads the vault
 * entry and passes the raw key material here.</p>
 *
 * <h3>Binary layout</h3>
 * Each call to {@link #encrypt(char[])} produces:
 * <pre>
 *   nonce (12 bytes) ‖ ciphertext (n bytes) ‖ tag (16 bytes)
 * </pre>
 * where the nonce is a fresh random 96-bit value per invocation (RFC
 * 5116 §3.1). The GCM authentication tag is always 128 bits (Java's
 * {@link Cipher} appends it transparently to ciphertext output).
 *
 * <h3>Security invariants</h3>
 * <ul>
 *   <li>Fresh nonce per encryption — never reused with the same key</li>
 *   <li>Master key loaded once at construction, never exposed via any
 *       method, never included in {@code toString()} or exception messages</li>
 *   <li>{@link #decrypt(byte[])} returns {@code char[]} to enable
 *       zeroing by the caller</li>
 *   <li>Exception messages never include plaintext token content</li>
 * </ul>
 *
 * @since 1.12.0
 */
public class AesGcmCredentialEncryptor implements CredentialEncryptor {

  static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  static final int GCM_NONCE_BYTES = 12;
  static final int GCM_TAG_BITS = 128;
  public static final int AES_256_KEY_BYTES = 32;

  private final SecretKey masterKey;
  private final SecureRandom secureRandom;

  /**
   * Creates the encryptor with the given AES master key.
   *
   * @param masterKey an AES SecretKey (must be 256 bits / 32 bytes)
   * @throws IllegalArgumentException if the key is null, has an
   *         unsupported algorithm, or has an incorrect key size
   */
  public AesGcmCredentialEncryptor(SecretKey masterKey) {
    this(masterKey, new SecureRandom());
  }

  // Visible for testing — allows injecting a deterministic SecureRandom
  AesGcmCredentialEncryptor(SecretKey masterKey, SecureRandom secureRandom) {
    this.masterKey = requireNonNull(masterKey, "masterKey must not be null");
    if (!"AES".equalsIgnoreCase(masterKey.getAlgorithm())) {
      throw new IllegalArgumentException(
          "masterKey algorithm must be AES, got: " + masterKey.getAlgorithm());
    }
    byte[] keyBytes = masterKey.getEncoded();
    if (keyBytes == null || keyBytes.length != AES_256_KEY_BYTES) {
      throw new IllegalArgumentException(
          "masterKey must be 256 bits (32 bytes), got: "
              + (keyBytes == null ? "null" : keyBytes.length + " bytes"));
    }
    this.secureRandom = requireNonNull(secureRandom,
        "secureRandom must not be null");
  }

  @Override
  public byte[] encrypt(@NonNull char[] plaintext) {
    requireNonNull(plaintext, "plaintext must not be null");

    byte[] nonce = new byte[GCM_NONCE_BYTES];
    secureRandom.nextBytes(nonce);

    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, masterKey,
          new GCMParameterSpec(GCM_TAG_BITS, nonce));
      byte[] ciphertextWithTags = cipher.doFinal(
          new String(plaintext).getBytes(StandardCharsets.UTF_8));

      // Layout: nonce (12) ‖ ciphertext+tag
      ByteBuffer output = ByteBuffer.allocate(
          GCM_NONCE_BYTES + ciphertextWithTags.length);
      output.put(nonce);
      output.put(ciphertextWithTags);
      return output.array();
    } catch (GeneralSecurityException e) {
      throw new ExternalCredentialEncryptorException(
          "Encryption failed", e);
    }
  }

  @Override
  public char[] decrypt(byte[] encrypted) {
    requireNonNull(encrypted, "encrypted must not be null");
    if (encrypted.length < GCM_NONCE_BYTES + GCM_TAG_BITS / 8) {
      throw new ExternalCredentialEncryptorException(
          "Encrypted data too short to contain nonce and authentication tag");
    }

    ByteBuffer buffer = ByteBuffer.wrap(encrypted);
    byte[] nonce = new byte[GCM_NONCE_BYTES];
    buffer.get(nonce);
    byte[] ciphertextWithTags = new byte[buffer.remaining()];
    buffer.get(ciphertextWithTags);

    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, masterKey,
          new GCMParameterSpec(GCM_TAG_BITS, nonce));
      byte[] plaintextBytes = cipher.doFinal(ciphertextWithTags);
      return new String(plaintextBytes, StandardCharsets.UTF_8).toCharArray();
    } catch (GeneralSecurityException e) {
      throw new ExternalCredentialEncryptorException(
          "Decryption failed — data may be corrupted or the master key "
              + "may have changed", e);
    }
  }

  /**
   * Exception thrown when credential encryption or decryption fails.
   *
   * <p>Never includes plaintext token content in the message.</p>
   *
   * @since 1.12.0
   */
  public static class ExternalCredentialEncryptorException
      extends RuntimeException {

    public ExternalCredentialEncryptorException(String message) {
      super(message);
    }

    public ExternalCredentialEncryptorException(String message,
        Throwable cause) {
      super(message, cause);
    }
  }

}
