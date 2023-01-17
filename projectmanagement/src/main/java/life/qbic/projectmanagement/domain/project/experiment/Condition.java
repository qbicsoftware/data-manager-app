package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  @SafeVarargs
  public static Condition create(VariableLevel<ExperimentalValue>... definedVariables) {
    Arrays.stream(definedVariables).forEach(Objects::requireNonNull);

    int distinctExperimentVariables = Arrays.stream(definedVariables).map(VariableLevel::experimentalVariable).collect(
        Collectors.toSet()).size();
    if (distinctExperimentVariables < definedVariables.length) {
      throw new IllegalArgumentException(
          "Variable levels are not from distinct experimental variables.");
    }

    return new Condition(definedVariables);
  }

  @SafeVarargs
  private Condition(VariableLevel<ExperimentalValue>... definedVariables) {
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

}
