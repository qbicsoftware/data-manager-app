package life.qbic.projectmanagement.domain.project;

import java.util.Objects;

/**
 * The experimental design description of a project
 *
 * @since 1.0.0
 */
public record ExperimentalDesignDescription(String value) {

  private static final long MAX_LENGTH = 1500;


  public ExperimentalDesignDescription {
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException("experimental design cannot be created from null");
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "To many characters (" + value.length()
              + "). The maximal length for experimental design descriptions is "
              + MAX_LENGTH);
    }
    Objects.requireNonNull(value);
  }

  public static ExperimentalDesignDescription create(String value) {
    return new ExperimentalDesignDescription(value);
  }

  public static long maxLength() {
    return MAX_LENGTH;
  }
}
