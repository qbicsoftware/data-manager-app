package life.qbic.projectmanagement.domain.project.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
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
  private final List<ExperimentalValue> levels = new ArrayList<>();

  private ExperimentalVariable(String name, ExperimentalValue... levels) {
    Arrays.stream(levels)
        .forEach(level -> Objects.requireNonNull(level, "only non-null levels expected"));
    Objects.requireNonNull(name);
    if (levels.length < 1) {
      throw new IllegalArgumentException("At least one variable level required.");
    }
    this.name = VariableName.create(name);
    for(ExperimentalValue level : levels) {
      if (hasDifferentUnitAsExistingLevels(level)) {
        throw new IllegalArgumentException(
            "experimental value not applicable. This variable has other levels without a unit or with a different unit.");
      }
      addLevel(level);
    }
  }

  public static ExperimentalVariable create(String name, ExperimentalValue... levels) {
    return new ExperimentalVariable(name, levels);
  }

  protected ExperimentalVariable() {
    // used by JPA
  }

  /**
   * Calling this method ensures that the experimental value is set as a level on the variable.
   *
   * @param experimentalValue the experimental value to be added to possible levels
   * @return the value added as level, a failed result otherwise
   * @throws IllegalArgumentException indicating that the unit of the provide level does not match
   *                                  with the unit of existing levels
   */
  Result<VariableLevel, Exception> addLevel(ExperimentalValue experimentalValue) {
    if (hasDifferentUnitAsExistingLevels(experimentalValue)) {
      return Result.fromError(new IllegalArgumentException(
          "experimental value not applicable. This variable has other levels without a unit or with a different unit."));
    }
    if (!levels.contains(experimentalValue)) {
      levels.add(experimentalValue);
    }
    VariableLevel variableLevel = new VariableLevel(name(), experimentalValue);
    return Result.fromValue(variableLevel);
  }

  private boolean hasDifferentUnitAsExistingLevels(ExperimentalValue experimentalValue) {
    if (levels.isEmpty()) {
      return false;
    }

    return !levels.stream().map(ExperimentalValue::unit).collect(Collectors.toSet())
        .contains(experimentalValue.unit());
  }

  public List<VariableLevel> levels() {
    return levels.stream()
        .map(experimentalValue -> VariableLevel.create(this.name, experimentalValue))
        .toList();
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
