package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * <b>Experimental Value</b>
 * <p>
 * Represents a value (aka level) that is part of an experimental variable and might have a unit.
 * <p>
 * Please note: As this is a value object, it must stay immutable.
 *
 * @since 1.0.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class ExperimentalValue {

  protected ExperimentalValue() {
    // used by JPA
  }

  /**
   * Creates a new instance of an experimental value object.
   *
   * @param value the value in textual representation
   * @param unit  the unit of the value if present.
   * @return a new experimental value
   * @since 1.0.0
   */
  public static ExperimentalValue create(String value, String unit) {
    return new ExperimentalValue(value, unit);
  }

  /**
   * Creates a new instance of an experimental value object without a unit.
   *
   * @param value the value in textual representation.
   * @return an instance of ExperimentalValue
   * @since 1.0.0
   */
  public static ExperimentalValue create(String value) {
    return new ExperimentalValue(value, null);
  }

  private String value;

  private String unit;

  ExperimentalValue(String value, String unit) {
    this.unit = unit;
    this.value = value;
  }

  public Optional<String> unit() {
    return Optional.ofNullable(unit);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExperimentalValue that = (ExperimentalValue) o;
    return Objects.equals(value, that.value) && Objects.equals(unit, that.unit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, unit);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ExperimentalValue.class.getSimpleName() + "[", "]")
        .add("value='" + value + "'")
        .add("unit='" + unit + "'")
        .toString();
  }
}
