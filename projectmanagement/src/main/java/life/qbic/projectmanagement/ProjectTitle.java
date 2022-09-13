package life.qbic.projectmanagement;

import java.util.Objects;

/**
 * The title of a project.
 * <li> Must not be empty or null
 */
public record ProjectTitle(String title) {

  public ProjectTitle {
    Objects.requireNonNull(title);
    if (title.isEmpty()) {
      throw new ProjectManagementDomainException("Project title " + title + " is empty or null.");
    }
  }

}
