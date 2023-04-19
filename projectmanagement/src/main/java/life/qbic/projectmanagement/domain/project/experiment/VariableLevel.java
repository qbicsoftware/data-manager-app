package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;
import java.util.StringJoiner;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import life.qbic.projectmanagement.domain.project.experiment.repository.jpa.VariableNameAttributeConverter;

/**
 * <b>Variable Level</b>
 * <p>
 * A container object that presents a certain level ({@link ExperimentalValue}) of an
 * {@link ExperimentalVariable} and is used in a{@link Condition} of a {@link ExperimentalGroup}.
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

  public VariableLevel(VariableName variableName,
      ExperimentalValue experimentalValue) {
    Objects.requireNonNull(variableName);
    Objects.requireNonNull(experimentalValue);

    this.experimentalValue = experimentalValue;
    this.variableName = variableName;
  }

  public static VariableLevel create(VariableName variableName,
      ExperimentalValue experimentalValue) {
    return new VariableLevel(variableName, experimentalValue);
  }

  protected VariableLevel() {
    // used for jpa
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

  @Override
  public String toString() {
    return new StringJoiner(", ", VariableLevel.class.getSimpleName() + "[", "]")
        .add("variableName=" + variableName)
        .add("experimentalValue=" + experimentalValue)
        .toString();
  }
}
