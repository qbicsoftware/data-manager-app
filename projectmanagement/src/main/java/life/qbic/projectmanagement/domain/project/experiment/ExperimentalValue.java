package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;
import java.util.Optional;

public interface ExperimentalValue {

  static ExperimentalValue create(String value, String unit) {
    return new SimpleExperimentalValue(value, unit);
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
