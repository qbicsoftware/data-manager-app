package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.lang.NonNull;

/**
 * <b>NGSMeasurementMetadata Identifier</b>
 *
 * <p>Unique identifier for measurements within QBiC's data management platform.</p>
 *
 * This identifier is usually not exposed to the user, but a technical identifier.
 *
 * @since 1.0.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class MeasurementId implements Serializable {

  @Serial
  private static final long serialVersionUID = 1841536150220843163L;

  @NonNull
  @Column(name = "measurement_id")
  private final String uuid;

  private MeasurementId(UUID id) {
    if (Objects.isNull(id)) {
      throw new IllegalArgumentException("uuid must be provided");
    }
    this.uuid = id.toString();
  }

  protected MeasurementId() {
    this(UUID.randomUUID());
    // needed for JPA
  }

  public static MeasurementId create() {
    return new MeasurementId(UUID.randomUUID());
  }

  public static MeasurementId of(UUID uid) {
    return new MeasurementId(uid);
  }

  public static MeasurementId parse(String uid) throws IllegalArgumentException {
    UUID id = UUID.fromString(uid);
    return new MeasurementId(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MeasurementId sampleId = (MeasurementId) o;
    return Objects.equals(uuid, sampleId.uuid);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SampleId.class.getSimpleName() + "[", "]")
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
