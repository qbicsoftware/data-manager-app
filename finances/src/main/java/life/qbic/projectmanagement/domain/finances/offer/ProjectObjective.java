package life.qbic.projectmanagement.domain.finances.offer;

import javax.persistence.AttributeConverter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record ProjectObjective(String objective) {

  public static ProjectObjective from(String objective) {
    return new ProjectObjective(objective);
  }

  public static class Converter implements AttributeConverter<ProjectObjective, String> {

    @Override
    public String convertToDatabaseColumn(ProjectObjective projectObjective) {
      return projectObjective.objective();
    }

    @Override
    public ProjectObjective convertToEntityAttribute(String s) {
      return ProjectObjective.from(s);
    }
  }

}
