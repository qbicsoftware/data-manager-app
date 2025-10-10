package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
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
  private List<VariableLevel> variableLevels;

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

  public List<VariableLevel> getVariableLevels() {
    return Collections.unmodifiableList(variableLevels);
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
    this.variableLevels = new ArrayList<>(variableLevels);
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
   * Checks if the variable level is contained in this condition
   *
   * @param level the level to check for
   * @return true if the condition contains the variable level, false otherwise
   */
  public boolean contains(VariableLevel level) {
    return variableLevels.contains(level);
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
    //we do not care about the order when comparing
    return new HashSet<>(variableLevels).equals(new HashSet<>(condition.variableLevels));
  }

  @Override
  public int hashCode() {
    return variableLevels.hashCode();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Condition.class.getSimpleName() + "[", "]")
        .add("variableLevels=" + variableLevels)
        .toString();
  }

  /**
   * Renames a variable defined in this condition. If the variable is not found, does nothing.
   *
   * @param oldName the old name of the variable
   * @param newName the name of the variable after renaming, must not be null or blank
   */
  void renameVariable(String oldName, String newName) {
    Objects.requireNonNull(newName, "New variable name cannot be null.");
    if (newName.isBlank()) {
      throw new IllegalArgumentException("New variable name cannot be blank.");
    }
    variableLevels.stream()
        .filter(it -> it.variableName().value().equals(oldName))
        .findAny()
        .ifPresent(variableLevel -> {
          VariableLevel replacement = new VariableLevel(VariableName.create(newName),
              variableLevel.experimentalValue());

          int index = variableLevels.indexOf(variableLevel);
          variableLevels.remove(variableLevel);
          variableLevels.add(index, replacement);
        });
  }

  public Condition deepCopy() {
    List<VariableLevel> copiedLevels = variableLevels.stream().map(
        it -> {
          ExperimentalValue experimentalValue = ExperimentalValue.create(
              it.experimentalValue().value(), it.experimentalValue().unit().orElse(null));
          return VariableLevel.create(VariableName.create(it.variableName().value()),
              experimentalValue);
        }).toList();
    return new Condition(copiedLevels);
  }
}
