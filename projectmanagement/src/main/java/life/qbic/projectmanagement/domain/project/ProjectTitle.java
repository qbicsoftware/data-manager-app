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
  }

  public static ProjectTitle create(String title) {
    return new ProjectTitle(title);
  }

  public static long maxLength() {
    return MAX_LENGTH;
  }

}
