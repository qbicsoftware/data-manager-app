package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;
import java.util.Optional;

/**
 * <b>Experimental Value</b>
 * <p>
 * Represents an value (aka level) that is part of an experimental variable and might have a unit.
 *
 * @since 1.0.0
 */
public interface ExperimentalValue {

  /**
   * Creates a new instance of an experimental value object.
   *
   * @param value the value in textual representation
   * @param unit  the unit of the value if present
   * @return
   * @since 1.0.0
   */
  static ExperimentalValue create(String value, String unit) {
    return new SimpleExperimentalValue(value, unit);
  }

  /**
   * Creates a new instance of an experimental value object without a unit.
   *
   * @param value the value in textual representation.
   * @return
   * @since 1.0.0
   */
  static ExperimentalValue create(String value) {
    return new SimpleExperimentalValue(value);
  }

  Optional<String> unit();

  String value();

  class SimpleExperimentalValue implements ExperimentalValue {

    private final String value;

    private final String unit;

    SimpleExperimentalValue(String value) {
      this.unit = null;
      this.value = value;
    }

    SimpleExperimentalValue(String value, String unit) {
      this.unit = unit;
      this.value = value;
    }

    @Override
    public Optional<String> unit() {
      return Optional.ofNullable(unit);
    }

    @Override
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
      SimpleExperimentalValue that = (SimpleExperimentalValue) o;
      return Objects.equals(value, that.value) && Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, unit);
    }
  }

}
