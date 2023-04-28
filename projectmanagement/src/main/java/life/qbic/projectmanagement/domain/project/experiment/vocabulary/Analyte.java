package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

import java.io.Serializable;
import java.util.Objects;

/**
 * <a href="https://medical-dictionary.thefreedictionary.com/analyte">analyte</a>: Any material or
 * chemical substance subjected to analysis.
 *
 * @param label a natural string representation of the {@link Analyte}
 */
public record Analyte(String label) implements Serializable {

  public Analyte {
    Objects.requireNonNull(label);
    if (label.isEmpty()) {
      throw new ProjectManagementDomainException("Vocabulary label for Analyte is empty.");
    }
  }

  public static Analyte create(String label) {
    return new Analyte(label);
  }

  public String value() {
    return this.label();
  }
}
