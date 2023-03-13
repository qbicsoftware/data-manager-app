package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
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
 * Conditions in an experimental design can be defined via {@link #defineCondition(String, VariableLevel[])}.
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

  @Convert(converter = ConditionLabelAttributeConverter.class)
  private ConditionLabel label;

  @ElementCollection(targetClass = VariableLevel.class, fetch = FetchType.EAGER)
  private List<VariableLevel> variableLevels;

  protected Condition() {
    //used by jpa
  }

  /**
   * Creates a new instance of a condition object with one or more {@link VariableLevel} defining
   * the level of an experimental variable.
   *
   * @param label            the label of the condition unique in the context of the experiment.
   * @param definedVariables the linear combination of experimental variable levels
   * @return the condition
   * @since 1.0.0
   */
  public static Condition create(String label,
      VariableLevel... definedVariables) {
    return new Condition(label, definedVariables);
  }


  private Condition(String label, VariableLevel... variableLevels) {
    Arrays.stream(variableLevels).forEach(Objects::requireNonNull);
    Objects.requireNonNull(label, "condition label must not be null");

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
    this.label = ConditionLabel.create(label);
    this.variableLevels = Arrays.stream(variableLevels).toList();
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
   * @return the label of the condition
   */
  public ConditionLabel label() {
    return label;
  }

  /**
   * Compares the defined {@link VariableLevel}s to the {@link VariableLevel}s of the other
   * condition. Ignores the order of the levels in the condition.
   *
   * @param other the other condition to compare the content of
   * @return true if <code>other</code> has the same levels defined; false otherwise
   */
  public boolean hasIdenticalContent(Condition other) {
    if (Objects.isNull(other)) {
      return false;
    }
    Comparator<VariableLevel> variableNameComparator = Comparator.comparing(
        l -> l.variableName().value());
    Stream<VariableLevel> sortedLevels = variableLevels.stream().sorted(variableNameComparator);
    return sortedLevels.equals(other.variableLevels.stream().sorted(variableNameComparator));
  }

  /**
   * {@inheritDoc}
   * <p>
   * ATTENTION: conditions are compared by their identity. They are neither compared by their label
   * nor by the levels they define. To compare the levels a condition defines please use
   * {@link Condition#hasIdenticalContent(Condition)}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Condition condition = (Condition) o;

    return conditionId == condition.conditionId;
  }

  /**
   * {@inheritDoc}
   * <p>
   * ATTENTION: conditions are compared by their identity. They are neither compared by their label
   * nor by the levels they define. To compare the levels a condition defines please us
   * {@link Condition#hasIdenticalContent(Condition)}
   */
  @Override
  public int hashCode() {
    return (int) (conditionId ^ (conditionId >>> 32));
  }
}
