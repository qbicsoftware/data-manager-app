package life.qbic.projectmanagement.domain.project;

import java.util.Objects;

/**
 * The title of a project.
 * <li> Must not be empty or null
 */

public record ProjectTitle(String title) {

  private static final long MAX_LENGTH = 180;

  public ProjectTitle {
    Objects.requireNonNull(title);
    if (title.isEmpty()) {
      throw new ProjectManagementDomainException("Project title is empty.");
    }
    if (title.length() > MAX_LENGTH) {
      throw new ProjectManagementDomainException(
          "Project title is too long. Allowed: " + MAX_LENGTH + "; Provided: " + title.length());
    }
  }

  public static ProjectTitle create(String title) {
    return new ProjectTitle(title);
  }

  public static long maxLength() {
    return MAX_LENGTH;
  }

}
