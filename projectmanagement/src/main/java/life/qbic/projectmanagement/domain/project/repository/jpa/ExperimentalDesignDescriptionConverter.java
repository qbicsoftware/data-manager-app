package life.qbic.projectmanagement.domain.project.repository.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;

@Converter(autoApply = true)
public class ExperimentalDesignDescriptionConverter implements
    AttributeConverter<ExperimentalDesignDescription, String> {


  @Override
  public String convertToDatabaseColumn(ExperimentalDesignDescription attribute) {
    return attribute.value();
  }

  @Override
  public ExperimentalDesignDescription convertToEntityAttribute(String dbData) {
    return ExperimentalDesignDescription.create(dbData);
  }
}
