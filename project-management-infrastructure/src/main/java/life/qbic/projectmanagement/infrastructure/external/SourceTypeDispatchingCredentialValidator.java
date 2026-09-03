package life.qbic.projectmanagement.infrastructure.external;

import java.util.Map;
import java.util.Objects;
import life.qbic.projectmanagement.application.associated_dataset.CredentialValidationException;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialValidator;
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

/**
 * Composite dispatcher that routes credential validation to the correct
 * provider-specific adapter based on source type.
 *
 * <p>New providers are added by implementing {@link CredentialValidatorAdapter}
 * and registering the new adapter in the constructor. No changes to the application service,
 * port, UI, or database are required.</p>
 *
 * <p>Thread-safe: the adapter map is immutable.</p>
 *
 * @since 1.12.0
 */
public class SourceTypeDispatchingCredentialValidator
    implements ExternalCredentialValidator {

  private final Map<SourceType, CredentialValidatorAdapter> adapters;

  public SourceTypeDispatchingCredentialValidator(
      Map<SourceType, CredentialValidatorAdapter> adapters) {
    Objects.requireNonNull(adapters, "adapters must not be null");
    if (adapters.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one credential validator adapter must be registered");
    }
    this.adapters = Map.copyOf(adapters);
  }

  @Override
  public boolean validateToken(SourceType sourceType, InstanceConfig config,
      char[] token) {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(config, "config must not be null");
    Objects.requireNonNull(token, "token must not be null");

    CredentialValidatorAdapter adapter = adapters.get(sourceType);
    if (adapter == null) {
      throw new CredentialValidationException(
          "No credential validator registered for source type: "
              + sourceType);
    }
    return adapter.validate(config, token);
  }

}
