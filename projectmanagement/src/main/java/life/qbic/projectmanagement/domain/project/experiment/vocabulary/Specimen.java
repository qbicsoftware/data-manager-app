package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record Specimen(String label) implements Serializable {

  public Specimen {
    Objects.requireNonNull(label);
    if (label.isEmpty()) {
      throw new ProjectManagementDomainException("Vocabulary label for Specimen is empty.");
    }
  }

  public static Specimen create(String label) {
    return new Specimen(label);
  }

  public String value() {
    return this.label();
  }

}
