package life.qbic.projectmanagement.domain.project.experiment.repository.jpa;

import java.util.Objects;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import life.qbic.projectmanagement.domain.project.experiment.VariableName;

/**
 * JPA converter for {@link VariableName} to String column.
 *
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class VariableNameAttributeConverter implements AttributeConverter<VariableName, String> {

  @Override
  public String convertToDatabaseColumn(VariableName attribute) {
    return attribute.value();
  }

  @Override
  public VariableName convertToEntityAttribute(String dbData) {
    return VariableName.create(dbData);
  }
}
