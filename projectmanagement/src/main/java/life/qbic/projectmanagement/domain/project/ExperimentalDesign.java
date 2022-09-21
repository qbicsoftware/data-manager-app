package life.qbic.projectmanagement.domain.project;

import java.util.Optional;

/**
 * The experimental design description of a project
 *
 * @since 1.0.0
 */
public class ExperimentalDesign {

  private String value;

  private ExperimentalDesign(String value) {
    this.value = value;
  }

  public static ExperimentalDesign of(String value) {
    return new ExperimentalDesign(value);
  }

  public Optional<String> value() {
    return Optional.ofNullable(value);
  }
}
