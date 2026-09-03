package life.qbic.datamanager.configuration;

import java.util.Base64;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import life.qbic.projectmanagement.application.associated_dataset.CredentialEncryptor;
import life.qbic.projectmanagement.application.associated_dataset.DatasetSource;
import life.qbic.projectmanagement.application.associated_dataset.DefaultExternalCredentialService;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialValidator;
import life.qbic.projectmanagement.application.associated_dataset.SourceInstanceRegistry;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.UserExternalCredentialRepository;
import life.qbic.projectmanagement.infrastructure.DataManagerVault;
import life.qbic.projectmanagement.infrastructure.config.JacksonConfig;
import life.qbic.projectmanagement.infrastructure.external.AesGcmCredentialEncryptor;
import life.qbic.projectmanagement.infrastructure.external.CredentialValidatorAdapter;
import life.qbic.projectmanagement.infrastructure.external.SourceTypeDispatchingCredentialValidator;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmClient;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmClient.InvenioRdmHttpClient;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmCredentialValidatorAdapter;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmDatasetSource;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmProperties;
import life.qbic.projectmanagement.infrastructure.external.invenio.PropertiesBackedSourceInstanceRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires up the InvenioRDM integration and credential management beans.
 *
 * <p>Registers:
 * <ul>
 *   <li>{@link InvenioRdmProperties} — config-bound properties
 *       ({@code qbic.external-service.invenio-rdm.*})</li>
 *   <li>{@link InvenioRdmClient} — low-level HTTP client (stateless,
 *       shared singleton)</li>
 *   <li>{@link SourceInstanceRegistry} — admin-configured instance
 *       lookup (ADR-0002 I2)</li>
 *   <li>{@link DatasetSource} — the port adapter for InvenioRDM
 *       (ADR-0002 P2)</li>
 *   <li>{@link CredentialEncryptor} — AES-256-GCM encryptor with
 *       dedicated master key from the PKCS12 vault (ADR-0002 S2)</li>
 *   <li>{@link ExternalCredentialValidator} — composite dispatcher
 *       routing per source type</li>
 *   <li>{@link ExternalCredentialService} — application-layer
 *       credential lifecycle orchestration</li>
 * </ul>
 *
 * @since 1.12.0
 */
@Configuration
@Import(value = JacksonConfig.class)
@EnableConfigurationProperties(InvenioRdmProperties.class)
public class InvenioRdmConfiguration {

  @Bean
  public InvenioRdmClient invenioRdmClient(
      @NonNull @Qualifier(value = "nullableFieldsObjectMapper") ObjectMapper objectMapper) {
    return new InvenioRdmHttpClient(objectMapper);
  }

  @Bean
  public SourceInstanceRegistry sourceInstanceRegistry(InvenioRdmProperties properties) {
    return new PropertiesBackedSourceInstanceRegistry(properties);
  }

  /**
   * Reads the dedicated master AES key from the PKCS12 vault and
   * constructs the encryptor. Fails fast at application startup if
   * the vault entry is missing — credential management cannot operate
   * without it.
   */
  @Bean
  public CredentialEncryptor credentialEncryptor(
      DataManagerVault vault,
      @Value("${qbic.security.vault.external-credential.key-alias}") String keyAlias) {
    // PKCS12 keystores force entry password = store password (the keystore key).
    // The vault entry was created via keytool, which silently used the keystore
    // password, so we must read it using the keystore password, not the
    // separate entry password used for entries created via DataManagerVault.add().
    String keyString = vault.read(keyAlias, true)
        .orElseThrow(() -> new IllegalStateException(
            "Vault entry not found for alias '" + keyAlias
                + "'. The external credential master key must be "
                + "provisioned in the PKCS12 keystore before the "
                + "application starts."));
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(keyString);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Vault entry for alias '" + keyAlias
              + "' is not valid Base64-encoded data. The key must be "
              + "stored as a Base64-encoded 32-byte AES key.", e);
    }
    if (keyBytes.length != AesGcmCredentialEncryptor.AES_256_KEY_BYTES) {
      throw new IllegalStateException(
          "Vault entry for alias '" + keyAlias
              + "' has incorrect key size: " + keyBytes.length
              + " bytes (expected " + AesGcmCredentialEncryptor.AES_256_KEY_BYTES
              + " for AES-256).");
    }
    SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
    return new AesGcmCredentialEncryptor(secretKey);
  }

  // ── Per-provider credential validator adapters ──────────────────

  @Bean
  public CredentialValidatorAdapter invenioRdmCredentialValidatorAdapter(
      InvenioRdmClient client) {
    return new InvenioRdmCredentialValidatorAdapter(client);
  }


  @Bean
  public ExternalCredentialValidator externalCredentialValidator(
      CredentialValidatorAdapter invenioRdmCredentialValidatorAdapter) {
    return new SourceTypeDispatchingCredentialValidator(Map.of(
        SourceType.INVENIO_RDM, invenioRdmCredentialValidatorAdapter
        // Future providers: add entries here, e.g.
        // SourceType.LIMS, new LimsCredentialValidatorAdapter(...)
    ));
  }


  @Bean
  public ExternalCredentialService externalCredentialService(
      ExternalCredentialValidator validator,
      UserExternalCredentialRepository credentialRepository,
      CredentialEncryptor encryptor,
      SourceInstanceRegistry registry) {
    return new DefaultExternalCredentialService(
        validator, credentialRepository, encryptor, registry);
  }

  @Bean
  public DatasetSource invenioRdmDatasetSource(
      InvenioRdmClient client,
      UserExternalCredentialRepository credentialRepository,
      CredentialEncryptor encryptor) {
    return new InvenioRdmDatasetSource(client, credentialRepository, encryptor);
  }

}
