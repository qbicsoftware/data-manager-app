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

}
