package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * The identifier of an experiment. This identifier is global and not restricted to a project. When
 * considering experiments it is always important to consider their context and to remember that
 * they can belong to one project max.
 */
@Embeddable
@Access(AccessType.FIELD)
public class ExperimentId implements Serializable {

  @Serial
  private static final long serialVersionUID = -7189266453060632052L;

  @Column(name = "id")
  private String uuid;

  private ExperimentId(UUID id) {
    this.uuid = id.toString();
  }

  protected ExperimentId() {
    //needed for JPA
  }

  public static ExperimentId create() {
    return new ExperimentId(UUID.randomUUID());
  }

  public static ExperimentId of(UUID uuid) {
    return new ExperimentId(uuid);
  }

  public static ExperimentId parse(String str) throws IllegalArgumentException {
    UUID id = UUID.fromString(str);
    return new ExperimentId(id);
  }

  @Override
  public String toString() {
    return uuid;
  }

  public static boolean isValid(String str) {
    try {
      UUID.fromString(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public String value() {
    return uuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ExperimentId that = (ExperimentId) o;

    return uuid.equals(that.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
