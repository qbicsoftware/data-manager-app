package life.qbic.projectmanagement.application.associated_dataset;

import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

/**
 * Validates a user's personal access token against an external data
 * source instance.
 *
 * <p>Separate from {@link DatasetSource} (ADR-0002 P2): token
 * validation is a credential-management concern, not a dataset
 * search/resolve concern.</p>
 *
 * <p>Implementations use source-type dispatching — the composite
 * implementation routes to the correct provider-specific adapter
 * based on the {@link SourceType}. The application service does not
 * need to know which validator handles a given provider; it passes
 * the source type and the dispatcher handles the rest.</p>
 *
 * <p>The application layer never sees, holds, or returns the plaintext
 * token. The only observable outcomes are a boolean (valid/invalid)
 * or a {@link CredentialValidationException} (transient failure).</p>
 *
 * @since 1.12.0
 */
public interface ExternalCredentialValidator {

  /**
   * Validates whether the given plaintext token is accepted by the
   * instance's authenticated-user endpoint.
   *
   * @param sourceType the external source type — routes to the
   *                   appropriate provider-specific validator
   * @param config     the target instance configuration
   * @param token      the plaintext token as {@code char[]} —
   *                   implementation <strong>MUST</strong> zero after use
   * @return {@code true} if the token is valid (the remote API
   *         returned 200), {@code false} if the server rejected the
   *         token (401/403)
   * @throws CredentialValidationException if the source type has no
   *         registered validator, or on transient failures (network
   *         error, server error after retries)
   */
  boolean validateToken(SourceType sourceType, InstanceConfig config,
      char[] token);

}
