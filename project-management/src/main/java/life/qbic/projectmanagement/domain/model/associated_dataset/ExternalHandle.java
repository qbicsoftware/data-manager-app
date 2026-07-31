package life.qbic.projectmanagement.domain.model.associated_dataset;

import java.util.Objects;

/**
 * External identity of a record on a remote source system.
 *
 * <p>An {@link ExternalHandle} is the stable, externally-visible identifier
 * that uniquely identifies a record on a particular source system instance.
 * For InvenioRDM this is typically the record ID or the DOI/PID that the
 * repository assigns. Combined with the {@link SourceType}, an
 * {@code ExternalHandle} is globally unique for a given dataset.</p>
 *
 * <p>The value is opaque at the domain level — it carries no assumption
 * about format (DOI, record ID, URL). Interpretation is delegated to
 * the source-specific adapter in infrastructure.</p>
 *
 * @since 1.12.0
 */
public final class ExternalHandle {

  private final String value;

  public ExternalHandle(String value) {
    Objects.requireNonNull(value, "External handle value must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("External handle value must not be blank");
    }
    this.value = value;
  }

  /**
   * The raw handle string (e.g. record ID, DOI, PID).
   */
  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ExternalHandle that = (ExternalHandle) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
