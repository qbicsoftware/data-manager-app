package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
 *
 * @since 1.0.0
 */
@Embeddable
//IMPORTANT: do not name the table condition; condition is a reserved keyword
public class Condition {

  @ElementCollection(targetClass = VariableLevel.class, fetch = FetchType.EAGER)
  private Set<VariableLevel> variableLevels;

  protected Condition() {
    //used by jpa
  }

  /**
   * Creates a new instance of a condition object with one or more {@link VariableLevel} defining
   * the level of an experimental variable.
   *
   * @param definedVariables the linear combination of experimental variable levels
   * @return the condition
   * @since 1.0.0
   */
  public static Condition create(Collection<VariableLevel> definedVariables) {
    return new Condition(definedVariables);
  }

  public Set<VariableLevel> getVariableLevels() {
    return Collections.unmodifiableSet(variableLevels);
  }

  private Condition(Collection<VariableLevel> variableLevels) {
    variableLevels.forEach(Objects::requireNonNull);

    if (variableLevels.isEmpty()) {
      throw new IllegalArgumentException("Please define at least one variable level.");
    }

    int distinctExperimentVariables = variableLevels.stream()
        .map(VariableLevel::variableName)
        .collect(Collectors.toSet())
        .size();

    if (distinctExperimentVariables < variableLevels.size()) {
      throw new IllegalArgumentException(
          "Variable levels are not from distinct experimental variables.");
    }
    // after performing checks on the list, we handle variable levels as a set, making comparisons cleaner and easier
    this.variableLevels = Set.copyOf(variableLevels);
  }

  /**
   * Queries the {@link ExperimentalValue} of an experimental variable. If the condition does not
   * contain a value for the experimental variable, the result will be empty.
   *
   * @param variableName the experimental variable to look for
   * @return the value if present, else empty
   * @since 1.0.0
   */
  public Optional<ExperimentalValue> valueOf(String variableName) {
    return variableLevels.stream()
        .filter(level -> level.variableName().value().equals(variableName))
        .map(VariableLevel::experimentalValue).findAny();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Condition)) {
      return false;
    }

    Condition condition = (Condition) o;

    return this.variableLevels.equals(condition.variableLevels);
  }

  @Override
  public int hashCode() {
    return variableLevels.hashCode();
  }
}
