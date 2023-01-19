package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * <b>Condition</b>
 * <p>
 * Conditions represent different linear combinations of experimental variable level(s). For example
 * you want to compare different genotypes of a specimen, or different treatments. Let's assume you
 * have a experimental design with two variables:
 * <ul>
 * <li>(1) the genotype (wildtype vs. mutant) </li>
 * <li>(2) a treatment a solvent with different concentrations (0 mmol/L and 150 mmol/L)</li>
 * </ul>
 * <p>
 * So in total there will be four conditions:
 *
 * <ul>
 *   <li>(1) wildtype + 0 mmol/L</li>
 *   <li>(2) wildtype + 150 mmol/L</li>
 *   <li>(3) mutant + 0 mmol/L</li>
 *   <li>(4) mutant + 150 mmol/L</li>
 * </ul>
 * <p>
 * Conditions in an experimental design can be defined via {@link ExperimentalDesign#createCondition(VariableLevel[])}.
 * <p>
 *
 * @since 1.0.0
 */
public class Condition {

  private final List<VariableLevel<ExperimentalValue>> definedVariables;

  private final Long id;

  /**
   * Creates a new instance of a condition object with one or more {@link VariableLevel} defining
   * the level of an experimental variable.
   *
   * @param definedVariables the linear combination of experimental variable levels
   * @return the condition
   * @since 1.0.0
   */
  @SafeVarargs
  public static Condition create(VariableLevel<ExperimentalValue>... definedVariables) {
    Arrays.stream(definedVariables).forEach(Objects::requireNonNull);

    if (definedVariables.length < 1) {
      throw new IllegalArgumentException("Please define at least one variable level.");
    }

    int distinctExperimentVariables = Arrays.stream(definedVariables)
        .map(VariableLevel::experimentalVariable).collect(
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

  /**
   * Queries the {@link ExperimentalValue} of an experimental variable. If the condition does not
   * contain a value for the experimental variable, the result will be empty.
   *
   * @param experimentalVariable the experimental variable to look for
   * @return the value if present, else empty
   * @since 1.0.0
   */
  public Optional<ExperimentalValue> valueOf(
      ExperimentalVariable<ExperimentalValue> experimentalVariable) {
    return definedVariables.stream().filter(
            variableValue -> variableValue.experimentalVariable().name()
                .equals(experimentalVariable.name()))
        .map(VariableLevel::experimentalValue).findAny();
  }

  /**
   * Lists all available experimental variables in the condition.
   *
   * @return A list of the experimental variables
   * @since 1.0.0
   */
  public List<ExperimentalVariable<ExperimentalValue>> experimentalVariables() {
    return definedVariables.stream().map(VariableLevel::experimentalVariable).toList();
  }

  /**
   * The identifier of the condition.
   *
   * @return
   * @since 1.0.0
   */
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
