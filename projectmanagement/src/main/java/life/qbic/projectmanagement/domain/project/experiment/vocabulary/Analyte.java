package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import java.io.Serializable;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

/**
 * An analyte is ... TODO add definition
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
