package life.qbic.projectmanagement.application.associated_dataset;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.ResourceMetadata;

/**
 * Port (SPI) for searching and resolving datasets on an external data
 * source system.
 *
 * <p>The application layer uses this interface to look for datasets and
 * fetch their metadata without knowing the specifics of the underlying
 * API (e.g. InvenioRDM REST API). Infrastructure adapters implement
 * this port for each supported source system.</p>
 *
 * <p>Each method accepts an {@code actingUserId} — the identity of the
 * user performing the action. Implementations use this identity to
 * resolve per-user credentials from infrastructure storage (vault,
 * encrypted DB column) inside the HTTP call scope only, and zero them
 * immediately after use (ADR-0002 D1: decryption boundary). The
 * application layer never sees, holds, or passes raw authentication
 * material.</p>
 *
 * <p>Stateless by design (ADR-0002 P2): no session concept. Each call
 * carries everything needed via {@link InstanceConfig}.</p>
 *
 * <p>Implementations must throw {@link DatasetSearchException} for
 * search failures and {@link DatasetResolveException} for metadata
 * resolve failures, rather than returning opaque nulls or using
 * generic {@link life.qbic.application.commons.ApplicationException}.</p>
 *
 * @since 1.12.0
 */
public interface DatasetSource {

  /**
   * Searches an external data source for datasets matching the query, on
   * behalf of the given user.
   *
   * <p>The {@code actingUserId} is used by the implementation to resolve
   * any per-user credentials needed for the search (e.g. a Personal
   * Access Token for restricted datasets on InvenioRDM). If the user has
   * no credentials configured, the implementation should return only
   * publicly accessible results.</p>
   *
   * @param query        the search parameters (query string, pagination)
   * @param config       the target instance (base URL, display name)
   * @param actingUserId the ID of the user performing the search
   * @return paginated search results
   * @throws DatasetSearchException if the search cannot be performed
   *         (network error, rate limit, server error, etc.)
   */
  SearchResult search(SearchQuery query, InstanceConfig config,
      String actingUserId) throws DatasetSearchException;

  /**
   * Resolves the full metadata for a single record on the source system,
   * on behalf of the given user.
   *
   * <p>Used when connecting a dataset: the search hit provides a
   * snapshot, and this method fetches the canonical metadata to persist
   * on the aggregate.</p>
   *
   * @param externalHandleValue the source-specific identifier for the
   *                            record (e.g. Zenodo record ID, DOI)
   * @param config              the target instance
   * @param actingUserId        the ID of the user performing the resolve
   * @return the resource metadata, or empty if the record was not found
   * @throws DatasetResolveException if the metadata cannot be retrieved
   *         (network error, rate limit, server error, etc.) —
   *         distinct from a missing record, which returns empty
   */
  Optional<ResourceMetadata> resolveMetadata(
      String externalHandleValue, InstanceConfig config,
      String actingUserId) throws DatasetResolveException;

  /**
   * Returns whether the given user has a valid (non-invalidated)
   * credential configured for the specified instance.
   *
   * <p>Used by the application service to enforce a hard gate before
   * connecting access-restricted datasets — access link creation
   * requires a valid PAT on the source system.</p>
   *
   * <p>Returns {@code true} only when a credential exists AND its
   * status is not {@code INVALIDATED}. No decryption or remote
   * validation is performed — this is a local presence check.</p>
   *
   * @param userId the user to check
   * @param config the target instance
   * @return {@code true} if a valid credential exists; {@code false}
   *         otherwise (no credential or invalidated)
   * @since 1.12.0
   */
  boolean hasValidCredential(String userId, InstanceConfig config);

  /**
   * Creates a sharable access link for a restricted record on the
   * source system, on behalf of the given user.
   *
   * <p>Used when connecting restricted datasets: the access link
   * allows project collaborators to view the dataset without
   * needing their own PAT. The link is stored in the dataset's
   * metadata snapshot.</p>
   *
   * <p>Requires the user to have permission to manage access links
   * on the record (typically the record owner). If the user lacks
   * permission, the implementation throws
   * {@link AccessLinkCreationException}.</p>
   *
   * @param externalHandleValue the source-specific identifier for the
   *                            record (e.g. Zenodo record ID, DOI)
   * @param config              the target instance
   * @param actingUserId        the ID of the user performing the action
   * @return the created link, carrying both the full access-link URL
   *         (record URL + ?token=<access-token>) and the source-system
   *         link id needed to {@linkplain #revokeAccessLink revoke} it
   *         later (or null id if the source does not expose one)
   * @throws AccessLinkCreationException if the link cannot be created
   *         (permission denied, network error, etc.)
   * @since 1.12.0
   */
  CreatedAccessLink createAccessLink(String externalHandleValue, InstanceConfig config,
      String actingUserId) throws AccessLinkCreationException;

  /**
   * Revokes (deletes) a previously created sharable access link on the
   * source system, on behalf of the given user.
   *
   * <p>Used to clean up an access link that was created during a connect
   * but which is no longer wanted: either the connect failed and was
   * rolled back, or the connection was subsequently removed from the
   * application. Revocation targets the link's {@code id} (not the token
   * embedded in the link URL), as returned by
   * {@link #createAccessLink} and stored on the metadata snapshot.</p>
   *
   * <p>Revoking an already-revoked (or never-created) link is treated as
   * success — the desired end state (no active link) is already reached —
   * so implementations must not throw for an absent link.</p>
   *
   * @param accessLinkId     the access link id on the source system
   * @param externalHandleValue the source-specific record identifier the
   *                            link belongs to (e.g. record ID)
   * @param config           the target instance
   * @param actingUserId     the ID of the user performing the action
   * @throws AccessLinkRevocationException if the link cannot be revoked
   *         (network error, server error, permission denied, etc.)
   * @since 1.12.0
   */
  void revokeAccessLink(String accessLinkId, String externalHandleValue,
      InstanceConfig config, String actingUserId)
      throws AccessLinkRevocationException;

}
