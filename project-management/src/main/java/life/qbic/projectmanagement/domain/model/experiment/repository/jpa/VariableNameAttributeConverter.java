package life.qbic.projectmanagement.domain.model.experiment.repository.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.model.experiment.VariableName;

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
    //Necessary since we expect VariableName to be non-null, but have no variableNames after project creation.
    if (dbData == null) {
      return null;
    }
    return VariableName.create(dbData);
  }
}
