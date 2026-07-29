package life.qbic.projectmanagement.domain.model.associated_dataset;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an {@link AssociatedDataset}.
 *
 * @since 1.12.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class AssociatedDatasetId implements Serializable {

  @Serial
  private static final long serialVersionUID = 6347490172839401058L;

  @Column(name = "associatedDatasetId")
  private final String uuid;

  protected AssociatedDatasetId() {
    // required by JPA
    this.uuid = UUID.randomUUID().toString();
  }

  private AssociatedDatasetId(UUID id) {
    Objects.requireNonNull(id, "uuid must be provided");
    this.uuid = id.toString();
  }

  public static AssociatedDatasetId create() {
    return new AssociatedDatasetId();
  }

  public static AssociatedDatasetId of(UUID uuid) {
    return new AssociatedDatasetId(uuid);
  }

  public static AssociatedDatasetId parse(String str) throws IllegalArgumentException {
    return new AssociatedDatasetId(UUID.fromString(str));
  }

  public String value() {
    return uuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssociatedDatasetId that = (AssociatedDatasetId) o;
    return Objects.equals(uuid, that.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }

  @Override
  public String toString() {
    return uuid;
  }
}
