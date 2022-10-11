package life.qbic.projectmanagement.domain.project.repository.jpa;

import javax.persistence.AttributeConverter;
import life.qbic.projectmanagement.domain.project.ProjectManager;

public class ProjectManagerConverter implements AttributeConverter<ProjectManager, String> {

  @Override
  public String convertToDatabaseColumn(ProjectManager attribute) {
    return attribute.fullName();
  }

  @Override
  public ProjectManager convertToEntityAttribute(String dbData) {
    return ProjectManager.of(dbData);
  }
}
