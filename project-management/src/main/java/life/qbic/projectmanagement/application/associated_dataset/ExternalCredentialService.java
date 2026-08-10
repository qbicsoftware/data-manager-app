package life.qbic.projectmanagement.application.associated_dataset;

import java.time.Instant;
import java.util.List;
import life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;

/**
 * Application service for managing user-level external provider
 * credentials.
 *
 * <p>Orchestrates token validation (via the composite
 * {@link ExternalCredentialValidator}), encryption and persistence
 * (delegated to infrastructure), and credential listing. The service is
 * source-agnostic — it operates on any configured instance regardless
 * of source type.</p>
 *
 * <p>The application service accepts a plaintext {@code char[]} token
 * from the view layer and is responsible for zeroing it before
 * returning (in a {@code finally} block). It never exposes the token
 * via return values or stored state.</p>
 *
 * @since 1.12.0
 */
public interface ExternalCredentialService {

  /**
   * Validates and persists a token for the given user and instance.
   *
   * <p>The plaintext token is zeroed before this method returns.
   * If a credential for this user/instance already exists, it is
   * replaced with the new validated token.</p>
   *
   * @param userId     the DM user adding the token
   * @param instanceId the target instance (e.g. {@code "zenodo"})
   * @param token      the plaintext token — <strong>MUST be
   *                   zeroed by this method</strong> before returning
   * @return result indicating success or failure
   */
  AddCredentialResult addCredential(String userId, String instanceId,
      char[] token);

  /**
   * Removes the token for the given user and instance.
   *
   * @param userId     the DM user removing the token
   * @param instanceId the target instance
   * @return {@code true} if a credential was removed, {@code false}
   *         if none existed for that user/instance pair
   */
  boolean removeCredential(String userId, String instanceId);

  /**
   * Lists the user's credential status for all configured instances.
   *
   * <p>Returns one entry per configured instance (from
   * {@link SourceInstanceRegistry}). Instances where the user has no
   * token configured are included with status
   * {@link CredentialStatus#NOT_CONFIGURED}.</p>
   *
   * @param userId the DM user whose credentials to list
   * @return credential status per instance; never null
   */
  List<CredentialStatusView> listCredentialStatuses(String userId);

  /**
   * Validates the currently stored token for a user and instance against
   * the external provider, and updates the stored status accordingly.
   *
   * <p>This method decrypts the stored encrypted token, sends it to the
   * provider's validation endpoint, and transitions the credential to
   * {@link life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus#VALID}
   * or
   * {@link life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus#INVALIDATED}
   * based on the response. Per ADR-0002, status transitions happen only
   * on explicit user-initiated validation.</p>
   *
   * @param userId     the DM user whose credential to validate
   * @param instanceId the target instance (e.g. {@code "zenodo"})
   * @return result indicating success or failure
   */
  AddCredentialResult validateCredential(String userId, String instanceId);

  // ── Result types ────────────────────────────────────────────────

  /**
   * Outcome of an {@link #addCredential} operation.
   *
   * <p>A sealed type hierarchy: only the three named record forms
   * are permitted. The plaintext token value is never present in
   * any result form.</p>
   */
  sealed interface AddCredentialResult
      permits Success, InvalidToken, ServiceError, UnknownInstance {
  }

  /** The token was validated and persisted successfully. */
  record Success() implements AddCredentialResult {
  }

  /** The token was rejected by the external instance. */
  record InvalidToken(String reason) implements AddCredentialResult {
  }

  /** A transient infrastructure failure prevented validation. */
  record ServiceError(String reason) implements AddCredentialResult {
  }

  /** No instance with the given ID is configured. */
  record UnknownInstance(String instanceId) implements AddCredentialResult {
  }

  /**
   * Credential status for a single instance (no plaintext token).
   */
  record CredentialStatusView(
      SourceType sourceType,
      String instanceId,
      String instanceDisplayName,
      String instanceBaseUrl,
      boolean configured,
      CredentialStatus status,
      Instant configuredAt
  ) {
  }

}
