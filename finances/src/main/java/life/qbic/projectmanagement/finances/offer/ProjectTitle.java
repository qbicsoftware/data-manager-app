package life.qbic.projectmanagement.finances.offer;

import javax.persistence.AttributeConverter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectTitle {

  private final String title;

  public ProjectTitle(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return title();
  }

  public String title() {
    return title;
  }

  public static class Converter implements AttributeConverter<ProjectTitle, String> {

    @Override
    public String convertToDatabaseColumn(ProjectTitle projectTitle) {
      return projectTitle.title();
    }

    @Override
    public ProjectTitle convertToEntityAttribute(String s) {
      return new ProjectTitle(s);
    }
  }
}
