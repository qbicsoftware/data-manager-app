package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class VariableLevel<T extends ExperimentalValue> {

  private final ExperimentalVariable<T> experimentalVariable;
  private final T experimentalValue;

  public VariableLevel(ExperimentalVariable<T> experimentalVariable, T experimentalValue) {
    Objects.requireNonNull(experimentalVariable);
    Objects.requireNonNull(experimentalValue);

    if (isValueMissingInVariableLevels(experimentalVariable, experimentalValue)) {
      throw new UnknownVariableLevelException(
          String.format("%s is not part of the experimental variable %s", experimentalValue.value(),
              experimentalVariable.name()));
    }

    this.experimentalVariable = experimentalVariable;
    this.experimentalValue = experimentalValue;
  }

  boolean isValueMissingInVariableLevels(ExperimentalVariable<T> experimentalVariable,
      T experimentalValue) {
    return experimentalVariable.values().stream().noneMatch(experimentalValue::equals);
  }

  public T experimentalValue() {
    return experimentalValue;
  }

  public ExperimentalVariable<T> experimentalVariable() {
    return experimentalVariable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VariableLevel<?> that = (VariableLevel<?>) o;
    return Objects.equals(experimentalVariable, that.experimentalVariable)
        && Objects.equals(experimentalValue, that.experimentalValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(experimentalVariable, experimentalValue);
  }
}
