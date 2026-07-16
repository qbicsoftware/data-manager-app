package life.qbic.projectmanagement.application.associated_dataset;

import java.util.Objects;

/**
 * Runtime configuration for a specific external data source instance,
 * passed to the {@link DatasetSource} port.
 *
 * <p>Bundles the static metadata needed to locate an instance (base URL,
 * display name). The invoking user's identity is passed separately to
 * the port methods — the application layer never handles authentication
 * material (per ADR-0002 D1: the decryption boundary is at infrastructure
 * only).</p>
 *
 * <p>Immutable value object.</p>
 *
 * @since 1.12.0
 */
public record InstanceConfig(
    /** Unique admin-assigned instance identifier (e.g. "zenodo"). */
    String id,

    /** Human-readable display name (e.g. "Zenodo (zenodo.org)"). */
    String displayName,

    /** Base URL of the instance (no trailing slash). */
    String baseUrl
) {

  public InstanceConfig {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(displayName, "displayName must not be null");
    Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    if (baseUrl.isBlank()) {
      throw new IllegalArgumentException("baseUrl must not be blank");
    }
  }
}
