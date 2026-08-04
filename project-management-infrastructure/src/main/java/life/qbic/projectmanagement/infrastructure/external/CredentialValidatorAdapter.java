package life.qbic.projectmanagement.infrastructure.external;

import life.qbic.projectmanagement.application.associated_dataset.CredentialValidationException;
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig;

/**
 * Provider-specific credential validation adapter.
 *
 * <p>Each external source type implements this interface with its own
 * authentication scheme and validation logic. The composite dispatcher
 * ({@code SourceTypeDispatchingCredentialValidator}) routes to the
 * correct adapter based on source type.</p>
 *
 * <p>This interface is the infrastructure-side counterpart of the
 * application-layer {@link life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialValidator}
 * port. It allows adding new provider types without changing the
 * dispatcher contract or the application service.</p>
 *
 * @since 1.12.0
 */
public interface CredentialValidatorAdapter {

  /**
   * Validates the given plaintext token against the instance.
   *
   * @param config the target instance configuration (base URL, display
   *               name)
   * @param token  the plaintext token as {@code char[]} — the
   *               implementation must not retain or expose the token
   *               value. The original array is <strong>not</strong>
   *               zeroed by the implementation; the caller owns the
   *               token lifecycle and is responsible for zeroing it
   *               after all operations are complete.
   * @return {@code true} if the token is valid (the remote API
   *         returned 200), {@code false} if the token was rejected
   *         (401/403)
   * @throws CredentialValidationException on transient failures
   *         (network error, 5xx server error after retries)
   */
  boolean validate(InstanceConfig config, char[] token);

}
