package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import life.qbic.projectmanagement.domain.project.experiment.repository.jpa.ConditionLabelAttributeConverter;

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
 * Conditions in an experimental design can be defined via {@link Experiment#defineCondition(String, VariableLevel[])}.
 * <p>
 *
 * @since 1.0.0
 */
@Entity(name = "conditions")
//IMPORTANT: do not name the table condition; condition is a reserved keyword
public class Condition {

  @Id
  @GeneratedValue
  private long conditionId;

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
  public static Condition create(VariableLevel... definedVariables) {
    return new Condition(definedVariables);
  }


  private Condition(VariableLevel... variableLevels) {
    Arrays.stream(variableLevels).forEach(Objects::requireNonNull);

    if (variableLevels.length < 1) {
      throw new IllegalArgumentException("Please define at least one variable level.");
    }

    int distinctExperimentVariables = Arrays.stream(variableLevels)
        .map(VariableLevel::variableName)
        .collect(Collectors.toSet())
        .size();
    if (distinctExperimentVariables < variableLevels.length) {
      throw new IllegalArgumentException(
          "Variable levels are not from distinct experimental variables.");
    }
    this.variableLevels = Arrays.stream(variableLevels).collect(Collectors.toUnmodifiableSet());
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

  /**
   * Compares the defined {@link VariableLevel}s to the {@link VariableLevel}s of the other
   * condition. Ignores the order of the levels in the condition.
   *
   * @param other the other condition to compare the content of
   * @return true if <code>other</code> has the same levels defined; false otherwise
   */
  public boolean hasSameLevelsDefined(Condition other) {
    return this.equals(other);
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
