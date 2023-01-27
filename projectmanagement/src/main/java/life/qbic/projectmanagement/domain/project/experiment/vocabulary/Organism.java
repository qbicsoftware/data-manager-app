package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

public record Organism(String label) {

  public Organism {
    Objects.requireNonNull(label);
    if (label.isEmpty()) {
      throw new ProjectManagementDomainException("Vocabulary label for Organism is empty.");
    }
  }

  public static Organism create(String label) {
    return new Organism(label);
  }

  public String value() {
    return this.label();
  }
}
