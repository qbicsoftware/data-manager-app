package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import java.io.Serializable;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

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
