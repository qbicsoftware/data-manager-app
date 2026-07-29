package life.qbic.projectmanagement.application.associated_dataset;

import java.util.Objects;

/**
 * Configuration descriptor for one available external data source instance,
 * typically loaded from application configuration (e.g.
 * {@code application.properties}).
 *
 * <p>This is the config-level record — it carries only the static metadata
 * needed to construct an {@link InstanceConfig} at runtime. Authentication
 * is handled entirely at the infrastructure layer (ADR-0002 D1): the
 * infrastructure adapter resolves per-user credentials from secure storage
 * when making external API calls.</p>
 *
 * <p>Admin-controlled: adding or changing instances is a config change +
 * deploy (ADR-0002 I2).</p>
 *
 * @since 1.12.0
 */
public record SourceInstanceDescriptor(
    String id,
    String displayName,
    String baseUrl
) {

  public SourceInstanceDescriptor {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(displayName, "displayName must not be null");
    Objects.requireNonNull(baseUrl, "baseUrl must not be null");
  }

  /**
   * Converts this descriptor into a runtime {@link InstanceConfig} without
   * an access token (anonymous access).
   */
  public InstanceConfig toInstanceConfig() {
    return new InstanceConfig(id, displayName, baseUrl);
  }
}
