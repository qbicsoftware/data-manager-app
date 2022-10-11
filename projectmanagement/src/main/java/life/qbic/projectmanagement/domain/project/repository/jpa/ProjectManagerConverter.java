package life.qbic.projectmanagement.domain.project.repository.jpa;

import java.util.Objects;
import javax.persistence.AttributeConverter;
import life.qbic.projectmanagement.domain.project.ProjectManager;

public class ProjectManagerConverter implements AttributeConverter<ProjectManager, String> {

  @Override
  public String convertToDatabaseColumn(ProjectManager attribute) {
    return attribute.fullName();
  }

  @Override
  public ProjectManager convertToEntityAttribute(String dbData) {
    if (Objects.isNull(dbData)) {
      return null;
    }
    return ProjectManager.of(dbData);
  }
}
