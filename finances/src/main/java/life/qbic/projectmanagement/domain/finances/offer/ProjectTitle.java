package life.qbic.projectmanagement.domain.finances.offer;

import java.util.Objects;
import javax.persistence.AttributeConverter;

/**
 * <b>Project Title</b>
 * <p>
 * Describes a project title associated with an offer
 *
 * @since 1.0.0
 */
public class ProjectTitle {

  private final String title;

  /**
   * Creates a new instance of an {@link ProjectTitle}
   *
   * @param title the value for the title
   * @return a new instance of project title
   * @since 1.0.0
   */
  public static ProjectTitle from(String title) {
    return new ProjectTitle(title);
  }

  private ProjectTitle(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return title();
  }

  public String title() {
    return title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectTitle that = (ProjectTitle) o;
    return Objects.equals(title, that.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title);
  }

  public static class Converter implements AttributeConverter<ProjectTitle, String> {

    @Override
    public String convertToDatabaseColumn(ProjectTitle projectTitle) {
      return projectTitle.title();
    }

    @Override
    public ProjectTitle convertToEntityAttribute(String s) {
      return ProjectTitle.from(s);
    }
  }
}
