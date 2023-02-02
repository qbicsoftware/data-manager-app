package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

import java.util.Objects;

public record Analyte(String label) {

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
