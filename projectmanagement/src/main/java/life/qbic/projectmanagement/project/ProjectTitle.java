package life.qbic.projectmanagement.project;

import life.qbic.projectmanagement.project.repository.jpa.ProjectTitleConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
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

  public static ProjectTitle from(String s) {
    return new ProjectTitle(s);
  }

  public String get() {
    return title();
  }
}
