package life.qbic.controlling.domain.model.experiment.repository.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.controlling.domain.model.experiment.ConditionLabel;

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
