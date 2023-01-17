package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
  public final Condition create(VariableLevel<ExperimentalValue>... definedVariables) {
    Arrays.stream(definedVariables).forEach(Objects::requireNonNull);
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
            .equals(experimentalVariable.name())).map(VariableLevel::experimentalValue).findAny();
  }

}
