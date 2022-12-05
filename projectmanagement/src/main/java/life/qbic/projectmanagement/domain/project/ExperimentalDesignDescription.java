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
    Objects.requireNonNull(value);
    if (value.isEmpty()) {
      throw new ProjectManagementDomainException("Experimental design is empty.");
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "To many characters (" + value.length()
              + "). The maximal length for experimental design descriptions is "
              + MAX_LENGTH);
    }
  }

  public static ExperimentalDesignDescription create(String value) {
    return new ExperimentalDesignDescription(value);
  }

  public static long maxLength() {
    return MAX_LENGTH;
  }
}
