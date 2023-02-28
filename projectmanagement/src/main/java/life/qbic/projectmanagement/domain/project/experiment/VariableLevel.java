package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import life.qbic.projectmanagement.domain.project.experiment.exception.UnknownVariableLevelException;
import life.qbic.projectmanagement.domain.project.experiment.repository.jpa.VariableNameAttributeConverter;

/**
 * <b>Variable Level</b>
 * <p>
 * A container object that presents a certain level ({@link ExperimentalValue}) of an
 * {@link ExperimentalVariable} and is used in a{@link Condition} of a {@link SampleGroup}.
 *
 * @since 1.0.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class VariableLevel {

  @Convert(converter = VariableNameAttributeConverter.class)
  private VariableName variableName;

  @Embedded
  private ExperimentalValue experimentalValue;

  public VariableLevel(ExperimentalVariable experimentalVariable,
      ExperimentalValue experimentalValue) {
    Objects.requireNonNull(experimentalVariable);
    Objects.requireNonNull(experimentalValue);

    if (isValueMissingInVariableLevels(experimentalVariable, experimentalValue)) {
      throw new UnknownVariableLevelException(
          String.format("%s is not part of the experimental variable %s", experimentalValue.value(),
              experimentalVariable.name()));
    }

//    this.variableId = experimentalVariable.id();
    this.experimentalValue = experimentalValue;
    this.variableName = experimentalVariable.name();
  }

  protected VariableLevel() {
    // used for jpa
  }

  private boolean isValueMissingInVariableLevels(ExperimentalVariable experimentalVariable,
      ExperimentalValue experimentalValue) {
    return experimentalVariable.levels().stream().noneMatch(experimentalValue::equals);
  }

  public VariableName variableName() {
    return variableName;
  }

  public ExperimentalValue experimentalValue() {
    // make sure the value is still final.
    // sadly can't use final keyword due to JPA reflections
    return experimentalValue.unit()
        .map(unit -> ExperimentalValue.create(experimentalValue.value(), unit))
        .orElseGet(() -> ExperimentalValue.create(experimentalValue.value()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VariableLevel that = (VariableLevel) o;
    return Objects.equals(variableName, that.variableName)
        && Objects.equals(experimentalValue, that.experimentalValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableName, experimentalValue);
  }
}
