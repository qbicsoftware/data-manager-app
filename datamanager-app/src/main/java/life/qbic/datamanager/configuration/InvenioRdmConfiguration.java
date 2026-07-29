package life.qbic.datamanager.configuration;

import java.nio.charset.StandardCharsets;
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
import life.qbic.projectmanagement.infrastructure.external.AesGcmCredentialEncryptor;
import life.qbic.projectmanagement.infrastructure.external.CredentialValidatorAdapter;
import life.qbic.projectmanagement.infrastructure.external.SourceTypeDispatchingCredentialValidator;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmClient;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmClient.InvenioRdmHttpClient;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmCredentialValidatorAdapter;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmDatasetSource;
import life.qbic.projectmanagement.infrastructure.external.invenio.InvenioRdmProperties;
import life.qbic.projectmanagement.infrastructure.external.invenio.PropertiesBackedSourceInstanceRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@EnableConfigurationProperties(InvenioRdmProperties.class)
public class InvenioRdmConfiguration {

  // ── Core integration beans ──────────────────────────────────────

  @Bean
  public InvenioRdmClient invenioRdmClient() {
    return new InvenioRdmHttpClient();
  }

  @Bean
  public SourceInstanceRegistry sourceInstanceRegistry(
      InvenioRdmProperties properties) {
    return new PropertiesBackedSourceInstanceRegistry(properties);
  }

  // ── Credential encryption (provider-agnostic) ───────────────────

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
    String keyString = vault.read(keyAlias)
        .orElseThrow(() -> new IllegalStateException(
            "Vault entry not found for alias '" + keyAlias
                + "'. The external credential master key must be "
                + "provisioned in the PKCS12 keystore before the "
                + "application starts."));
    SecretKey secretKey = new SecretKeySpec(
        keyString.getBytes(StandardCharsets.UTF_8), "AES");
    return new AesGcmCredentialEncryptor(secretKey);
  }

  // ── Per-provider credential validator adapters ──────────────────

  @Bean
  public CredentialValidatorAdapter invenioRdmCredentialValidatorAdapter(
      InvenioRdmClient client) {
    return new InvenioRdmCredentialValidatorAdapter(client);
  }

  // ── Composite credential validation dispatcher ──────────────────

  @Bean
  public ExternalCredentialValidator externalCredentialValidator(
      CredentialValidatorAdapter invenioRdmCredentialValidatorAdapter) {
    return new SourceTypeDispatchingCredentialValidator(Map.of(
        SourceType.INVENIO_RDM, invenioRdmCredentialValidatorAdapter
        // Future providers: add entries here, e.g.
        // SourceType.LIMS, new LimsCredentialValidatorAdapter(...)
    ));
  }

  // ── Application service ─────────────────────────────────────────

  @Bean
  public ExternalCredentialService externalCredentialService(
      ExternalCredentialValidator validator,
      UserExternalCredentialRepository credentialRepository,
      CredentialEncryptor encryptor,
      SourceInstanceRegistry registry) {
    return new DefaultExternalCredentialService(
        validator, credentialRepository, encryptor, registry);
  }

  // ── Dataset source adapter (wired with credential support) ──────

  @Bean
  public DatasetSource invenioRdmDatasetSource(
      InvenioRdmClient client,
      UserExternalCredentialRepository credentialRepository,
      CredentialEncryptor encryptor) {
    return new InvenioRdmDatasetSource(client, credentialRepository, encryptor);
  }

}
