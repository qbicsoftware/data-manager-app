package life.qbic.controlling.domain.finances.offer;

import jakarta.persistence.AttributeConverter;

/**
 * The project objective of an offer
 *
 * @param objective the project objective
 * @since 1.0.0
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
