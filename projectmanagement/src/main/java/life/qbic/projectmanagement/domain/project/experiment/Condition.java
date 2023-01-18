package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Condition {

  private final List<VariableLevel<ExperimentalValue>> definedVariables;

  private final Long id;


  @SafeVarargs
  public static Condition create(VariableLevel<ExperimentalValue>... definedVariables) {
    Arrays.stream(definedVariables).forEach(Objects::requireNonNull);

    if (definedVariables.length < 1) {
      throw new IllegalArgumentException("Please define at least one variable level.");
    }

    int distinctExperimentVariables = Arrays.stream(definedVariables).map(VariableLevel::experimentalVariable).collect(
        Collectors.toSet()).size();
    if (distinctExperimentVariables < definedVariables.length) {
      throw new IllegalArgumentException(
          "Variable levels are not from distinct experimental variables.");
    }

    return new Condition(new Random().nextLong(), definedVariables);
  }



  @SafeVarargs
  private Condition(Long id, VariableLevel<ExperimentalValue>... definedVariables) {
    this.id = id;
    this.definedVariables = Arrays.stream(definedVariables).toList();
  }

  public Optional<ExperimentalValue> valueOf(
      ExperimentalVariable<ExperimentalValue> experimentalVariable) {
    return definedVariables.stream().filter(
            variableValue -> variableValue.experimentalVariable().name()
                .equals(experimentalVariable.name()))
        .map(VariableLevel::experimentalValue).findAny();
  }

  public List<ExperimentalVariable<ExperimentalValue>> experimentalVariables() {
    return definedVariables.stream().map(VariableLevel::experimentalVariable).toList();
  }

  public Long id() {
    return this.id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Condition condition = (Condition) o;
    return id.equals(condition.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
