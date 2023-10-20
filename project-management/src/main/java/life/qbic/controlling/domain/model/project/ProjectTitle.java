package life.qbic.controlling.domain.model.project;

import java.util.Objects;
import life.qbic.application.commons.ApplicationException;

/**
 * The title of a project.
 * <li> Must not be empty or null
 */

public record ProjectTitle(String title) {

  private static final long MAX_LENGTH = 180;

  public ProjectTitle {
    Objects.requireNonNull(title);
    if (title.isEmpty()) {
      throw new ApplicationException("Project title is empty.");
    }
    if (title.length() > MAX_LENGTH) {
      throw new ApplicationException(
          "Project title is too long. Allowed: " + MAX_LENGTH + "; Provided: " + title.length());
    }
  }

  public static ProjectTitle of(String title) {
    return new ProjectTitle(title);
  }

  public static long maxLength() {
    return MAX_LENGTH;
  }

}
