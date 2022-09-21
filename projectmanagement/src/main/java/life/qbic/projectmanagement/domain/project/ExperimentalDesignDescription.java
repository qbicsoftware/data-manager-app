package life.qbic.projectmanagement.domain.project;

import java.util.Objects;

/**
 * The experimental design description of a project
 *
 * @since 1.0.0
 */
public record ExperimentalDesignDescription(String value) {


  public ExperimentalDesignDescription {
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException("experimental design cannot be created from null");
    }
    Objects.requireNonNull(value);
  }

  public static ExperimentalDesignDescription create(String value) {
    return new ExperimentalDesignDescription(value);
  }
}
