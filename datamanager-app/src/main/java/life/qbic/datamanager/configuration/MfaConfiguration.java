package life.qbic.datamanager.configuration;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import life.qbic.datamanager.security.mfa.AesGcmSecondFactorSecretProtector;
import life.qbic.identity.application.mfa.SecondFactorSecretProtector;
import life.qbic.projectmanagement.infrastructure.DataManagerVault;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <b>Second-factor configuration</b>
 *
 * <p>Wires the TOTP secret protector to the PKCS12 vault (ADR 0011 F1). The key is read under its own
 * alias so that factor material is rotatable and restorable without touching the external-credential
 * keys — a vault-key loss then degrades TOTP for everyone (risk R8) without also destroying the
 * stored Invenio credentials.</p>
 *
 * <p>Note what is <em>not</em> here yet: {@code MfaPolicy} (mode and grace window). It lands with the
 * first reader, the login challenge in M4-1, because a configuration property that nothing consumes
 * looks like an implemented feature and is not one.</p>
 *
 * @since 1.0.0
 */
@Configuration
public class MfaConfiguration {

  private static final int AES_256_KEY_BYTES = 32;

  /**
   * @param vault    the PKCS12 vault
   * @param keyAlias the dedicated alias, provisioned per deployment
   * @return the protector used by the enrolment service
   */
  @Bean
  public SecondFactorSecretProtector secondFactorSecretProtector(
      DataManagerVault vault,
      @Value("${qbic.security.vault.second-factor.key-alias}") String keyAlias) {
    // The supplier is invoked on first use, not here: see AesGcmSecondFactorSecretProtector's
    // documentation for why an unprovisioned alias must not stop the application from starting.
    return new AesGcmSecondFactorSecretProtector(
        () -> readAes256Key(vault, keyAlias), keyAlias);
  }

  /**
   * Reads a Base64-encoded AES-256 key from the vault.
   *
   * <p>Same shape and the same validation as the external-credential key in
   * {@code InvenioRdmConfiguration}; duplicated rather than shared so that a change to one credential
   * class cannot silently alter the other's key handling.</p>
   *
   * @throws IllegalStateException if the entry is missing, not Base64, or not 32 bytes — the caller
   *     turns this into "second factor unavailable", never into "no second factor"
   */
  private static SecretKey readAes256Key(DataManagerVault vault, String keyAlias) {
    // PKCS12 keystores force entry password = store password, so read with the store password
    // (see the note in InvenioRdmConfiguration for the keytool history behind this).
    String keyString = vault.read(keyAlias, true)
        .orElseThrow(() -> new IllegalStateException(
            "Vault entry not found for alias '" + keyAlias
                + "'. The second-factor master key must be provisioned in the PKCS12 keystore "
                + "before second-factor enrolment is used."));
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(keyString);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Vault entry for alias '" + keyAlias
              + "' is not valid Base64-encoded data. The key must be stored as a Base64-encoded "
              + "32-byte AES key.", e);
    }
    if (keyBytes.length != AES_256_KEY_BYTES) {
      throw new IllegalStateException(
          "Vault entry for alias '" + keyAlias + "' has incorrect key size: " + keyBytes.length
              + " bytes (expected " + AES_256_KEY_BYTES + " for AES-256).");
    }
    return new SecretKeySpec(keyBytes, "AES");
  }
}
