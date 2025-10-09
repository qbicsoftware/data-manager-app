package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.repository.jpa.VariableNameAttributeConverter;
import org.hibernate.annotations.NaturalId;

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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long variableId;

  @Convert(converter = VariableNameAttributeConverter.class)
  @Column(name = "name")
  @NaturalId(mutable = true)
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
    for (ExperimentalValue level : levels) {
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
  boolean addLevel(ExperimentalValue experimentalValue) {
    if (hasDifferentUnitAsExistingLevels(experimentalValue)) {
      throw new IllegalArgumentException(
          "experimental value not applicable. This variable has other levels without a unit or with a different unit.");
    }
    if (!levels.contains(experimentalValue)) {
      levels.add(experimentalValue);
      return true;
    }
    return false;
  }

  /**
   * Replaces the current levels with the list of provided levels. The provided list will not be
   * modified.
   *
   * @param levels the new levels for this variable. The provided list is copied and modifications
   *               to it are not reflected in the experimental variable.
   * @return true if the variable was changed as a result of this call, false otherwise.
   */
  boolean replaceLevels(List<ExperimentalValue> levels) {
    var workingCopy = levels.stream().map(
        it -> new ExperimentalValue(it.value(), it.unit().orElse(null))).toList();
    if (workingCopy.equals(this.levels)) {
      return false;
    }
    this.levels.clear();
    this.levels.addAll(workingCopy);
    return true;
  }


  void renameTo(String newName) {
    this.name = new VariableName(newName);
  }

  private boolean hasDifferentUnitAsExistingLevels(ExperimentalValue experimentalValue) {
    if (levels.isEmpty()) {
      return false;
    }
    return !usedUnit().equals(experimentalValue.unit());
  }

  public Optional<String> usedUnit() {
    return levels.stream()
        .map(ExperimentalValue::unit)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
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
    return Long.hashCode(variableId);
  }
}
