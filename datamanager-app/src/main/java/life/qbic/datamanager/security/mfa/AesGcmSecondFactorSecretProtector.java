package life.qbic.datamanager.security.mfa;

import static java.util.Objects.requireNonNull;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import life.qbic.identity.application.mfa.SecondFactorSecretProtector;
import life.qbic.identity.application.mfa.SecondFactorSecretUnavailableException;
import life.qbic.projectmanagement.infrastructure.external.AesGcmCredentialEncryptor;
import life.qbic.projectmanagement.infrastructure.external.AesGcmCredentialEncryptor.ExternalCredentialEncryptorException;
import life.qbic.projectmanagement.application.associated_dataset.CredentialEncryptor;

/**
 * <b>AES-256-GCM protection for TOTP seeds</b>
 *
 * <p>Adapter for {@link SecondFactorSecretProtector}, built on the <em>existing, reviewed</em>
 * AES-GCM implementation ({@link AesGcmCredentialEncryptor}) rather than a second copy of it. Only
 * the key differs: this one is supplied under the dedicated
 * {@code qbic.security.vault.second-factor.key-alias}, so factor material can be rotated and restored
 * independently of external-provider credentials (ADR 0011 F1).</p>
 *
 * <p>Lives in the composition root rather than in {@code identity-infrastructure} on purpose: the
 * encryptor belongs to the project-management context, and making the identity context depend on it
 * would be a layering violation for the sake of avoiding 40 lines of Base64.</p>
 *
 * <h3>Why the key is resolved lazily</h3>
 * <p>Resolving it in the constructor would make the application fail to start on every deployment
 * where the new vault alias has not been provisioned yet — including all of them, today, because
 * nothing enrols a factor until M4 wires the flow. The key is therefore read on first use and cached;
 * a missing or malformed key then fails the operation that needs it
 * ({@link SecondFactorSecretUnavailableException}) instead of the whole service. That is fail-closed
 * where it matters (a factor that cannot be decrypted is never treated as valid) without turning an
 * unprovisioned optional feature into an outage. When the login path starts depending on factors
 * (M4-1), this becomes an eager startup check.</p>
 *
 * @since 1.0.0
 */
public class AesGcmSecondFactorSecretProtector implements SecondFactorSecretProtector {

  private static final int AES_256_KEY_BYTES = 32;

  private final Supplier<SecretKey> masterKeySupplier;
  private final String keyAlias;
  private final AtomicReference<CredentialEncryptor> encryptor = new AtomicReference<>();

  /**
   * @param masterKeySupplier resolves the AES-256 master key, e.g. by reading the PKCS12 vault; called
   *     once, on first use
   * @param keyAlias          the vault alias, used in diagnostics only — never key material
   */
  public AesGcmSecondFactorSecretProtector(Supplier<SecretKey> masterKeySupplier, String keyAlias) {
    this.masterKeySupplier = requireNonNull(masterKeySupplier, "masterKeySupplier must not be null");
    this.keyAlias = requireNonNull(keyAlias, "keyAlias must not be null");
  }

  @Override
  public String protect(char[] plaintext) {
    requireNonNull(plaintext, "plaintext must not be null");
    return Base64.getEncoder().encodeToString(encryptor().encrypt(plaintext));
  }

  @Override
  public char[] reveal(String protectedSecret) {
    requireNonNull(protectedSecret, "protectedSecret must not be null");
    byte[] blob;
    try {
      blob = Base64.getDecoder().decode(protectedSecret);
    } catch (IllegalArgumentException e) {
      throw new SecondFactorSecretUnavailableException(
          "the stored second-factor secret is not Base64 and cannot be decrypted", e);
    }
    try {
      return encryptor().decrypt(blob);
    } catch (ExternalCredentialEncryptorException e) {
      // Wrong key, truncated ciphertext or a failed GCM tag: the seed is unavailable, and the caller
      // must treat the factor as unusable rather than guess (AUTH-NFR-02).
      throw new SecondFactorSecretUnavailableException(
          "the second-factor secret could not be decrypted (vault alias '" + keyAlias + "')", e);
    }
  }

  private CredentialEncryptor encryptor() {
    CredentialEncryptor current = encryptor.get();
    if (current != null) {
      return current;
    }
    synchronized (this) {
      if (encryptor.get() == null) {
        SecretKey key;
        try {
          key = masterKeySupplier.get();
        } catch (RuntimeException e) {
          throw new SecondFactorSecretUnavailableException(
              "the second-factor master key (vault alias '" + keyAlias + "') is unavailable", e);
        }
        if (key.getEncoded() == null || key.getEncoded().length != AES_256_KEY_BYTES) {
          throw new SecondFactorSecretUnavailableException(
              "the second-factor master key (vault alias '" + keyAlias
                  + "') is not a 256-bit AES key");
        }
        encryptor.set(new AesGcmCredentialEncryptor(key));
      }
      return encryptor.get();
    }
  }
}
