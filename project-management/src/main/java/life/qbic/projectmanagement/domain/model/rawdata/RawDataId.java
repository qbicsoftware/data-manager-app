package life.qbic.projectmanagement.domain.model.rawdata;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.lang.NonNull;

/**
 * <b>Raw Data Identifier</b>
 *
 * <p>Unique identifier for raw data generated from measurements within QBiC's data management platform.</p>
 *
 * This identifier is usually not exposed to the user, but a technical identifier.
 *
 * @since 1.0.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class RawDataId implements Serializable {

  @Serial
  private static final long serialVersionUID = -4310569534537839626L;

  @NonNull
  @Column(name = "rawdata_id")
  private final String uuid;

  private RawDataId(UUID id) {
    if (Objects.isNull(id)) {
      throw new IllegalArgumentException("uuid must be provided");
    }
    this.uuid = id.toString();
  }

  protected RawDataId() {
    this(UUID.randomUUID());
    // needed for JPA
  }

  public static RawDataId create() {
    return new RawDataId(UUID.randomUUID());
  }

  public static RawDataId of(UUID uid) {
    return new RawDataId(uid);
  }

  public static RawDataId parse(String uid) throws IllegalArgumentException {
    UUID id = UUID.fromString(uid);
    return new RawDataId(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RawDataId rawDataId = (RawDataId) o;
    return Objects.equals(uuid, rawDataId.uuid);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", RawDataId.class.getSimpleName() + "[", "]")
        .add("uuid=" + uuid)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }

  public String value() {
    return this.uuid;
  }

}
