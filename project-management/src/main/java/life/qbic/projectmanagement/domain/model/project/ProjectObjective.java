package life.qbic.projectmanagement.domain.model.project;

import java.util.Objects;
import life.qbic.application.commons.ApplicationException;

/**
 * The objective of a project.
 * <li> Must not be empty or null
 *
 * @since 1.0.0
 */
public record ProjectObjective(String objective) {

  private static final long MAX_LENGTH = 2000;

  public ProjectObjective {
    Objects.requireNonNull(objective);
    if (objective.isEmpty()) {
      throw new ApplicationException("Project objective is empty.");
    }
    if (objective.length() > MAX_LENGTH) {
      throw new ApplicationException(
          "Project objective is too long. Allowed max size: " + MAX_LENGTH
              + "; Provided size: " + objective.length());
    }
  }

  public static ProjectObjective create(String objective) {
    return new ProjectObjective(objective);
  }

  public String value() {
    return this.objective();
  }

  public static long maxLength() {
    return MAX_LENGTH;
  }
}
