package life.qbic.projectmanagement.domain.project.experiment.repository.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import life.qbic.projectmanagement.domain.project.experiment.ConditionLabel;

/**
 * Converts condition labels to database column
 */
@Converter(autoApply = true)
public class ConditionLabelAttributeConverter implements
    AttributeConverter<ConditionLabel, String> {

  @Override
  public String convertToDatabaseColumn(ConditionLabel attribute) {
    return attribute.value();
  }

  @Override
  public ConditionLabel convertToEntityAttribute(String dbData) {
    return ConditionLabel.create(dbData);
  }
}
