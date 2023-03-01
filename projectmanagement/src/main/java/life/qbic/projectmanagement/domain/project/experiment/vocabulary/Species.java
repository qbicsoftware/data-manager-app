package life.qbic.projectmanagement.domain.project.experiment.vocabulary;

import java.io.Serializable;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectManagementDomainException;

/**
 * A species is ... TODO add definition
 *
 * @param label a natural string representation of the {@link Species}
 */
public record Species(String label) implements Serializable {

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
