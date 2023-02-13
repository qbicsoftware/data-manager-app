package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

public record Species(String label) {

  public Species {
    Objects.requireNonNull(label);
    if (label.isEmpty()) {
      throw new ProjectManagementDomainException("Vocabulary label for Species is empty.");
    }
  }

  public static Species create(String label) {
    return new Species(label);
  }

  public String value() {
    return this.label();
  }
}
