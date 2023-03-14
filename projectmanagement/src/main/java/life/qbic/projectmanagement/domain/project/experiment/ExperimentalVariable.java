package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.exception.VariableLevelExistsException;
import life.qbic.projectmanagement.domain.project.experiment.repository.jpa.VariableNameAttributeConverter;

/**
 * <b>Experimental Variable</b>
 * <p>
 * Describes an experimental variable with a unique and declarative name. In addition, it contains
 * {@link ExperimentalValue}s, representing the levels of the variable that are part of the
 * experiment.
 * <p>
 * Experimental variables can be created via the
 * {@link Experiment#addVariableToDesign(String, List)} function.
 *
 * @since 1.0.0
 */
@Entity(name = "experimental_variables")
public class ExperimentalVariable {

  @Id
  @GeneratedValue
  private long variableId;

  @Convert(converter = VariableNameAttributeConverter.class)
  @Column(name = "name")
  private VariableName name;

  @ElementCollection(fetch = FetchType.EAGER)
  private final List<ExperimentalValue> levels;

  private ExperimentalVariable(String name, ExperimentalValue... levels) {
    Arrays.stream(levels)
        .forEach(level -> Objects.requireNonNull(level, "only non-null levels expected"));
    Objects.requireNonNull(name);
    if (levels.length < 1) {
      throw new IllegalArgumentException("At least one variable level required.");
    }
    this.name = VariableName.create(name);
    this.levels = List.of(levels);
  }

  public static ExperimentalVariable create(String name, ExperimentalValue... levels) {
    return new ExperimentalVariable(name, levels);
  }

  protected ExperimentalVariable() {
    // used by JPA
    levels = new ArrayList<>();
  }

  /**
   * Calling this method ensures that the experimental value is set as a level on the variable.
   *
   * @param experimentalValue the experimental value to be added to possible levels
   * @return the value added as level, a failed result otherwise
   * @throws IllegalArgumentException indicating that the unit of the provide level does not match
   *                                  with the unit of existing levels
   */
  Result<ExperimentalValue, RuntimeException> addLevel(ExperimentalValue experimentalValue) {
    if (hasDifferentUnitAsExistingLevels(experimentalValue)) {
      return Result.failure(new IllegalArgumentException(
          "experimental value not applicable. This variable has other levles without a unit or with a different unit."));
    }
    if (!levels.contains(experimentalValue)) {
      // the level is already part of the variable. No action needed
      levels.add(experimentalValue);
      return Result.success(experimentalValue);
    } else {
      return Result.failure(new VariableLevelExistsException(
          "the variable " + this.name() + " already allows for this level."
      ));
    }
  }

  private boolean hasDifferentUnitAsExistingLevels(ExperimentalValue experimentalValue) {
    Predicate<ExperimentalValue> hasDifferentUnit = input -> !input.unit()
        .equals(experimentalValue.unit());
    return levels.stream().anyMatch(hasDifferentUnit);
  }

  public List<ExperimentalValue> levels() {
    return levels.stream().toList();
  }

  public VariableName name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ExperimentalVariable that = (ExperimentalVariable) o;

    return variableId == that.variableId;
  }

  @Override
  public int hashCode() {
    return (int) (variableId ^ (variableId >>> 32));
  }
}
