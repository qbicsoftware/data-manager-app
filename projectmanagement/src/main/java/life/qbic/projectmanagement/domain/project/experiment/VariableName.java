package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;

/**
 * The name of a variable
 */
public record VariableName(String value) {

  public VariableName {
    Objects.requireNonNull(value, "Variable name must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("A variable name must not be blank");
    }
  }

  public static VariableName create(String name) {
    return new VariableName(name);
  }

}
