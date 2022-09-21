package life.qbic.projectmanagement.domain.project.repository.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import life.qbic.projectmanagement.domain.project.ExperimentalDesign;

@Converter(autoApply = true)
public class ExperimentalDesignConverter implements AttributeConverter<ExperimentalDesign, String> {


  @Override
  public String convertToDatabaseColumn(ExperimentalDesign attribute) {
    return attribute.value().orElse("");
  }

  @Override
  public ExperimentalDesign convertToEntityAttribute(String dbData) {
    return ExperimentalDesign.of(dbData);
  }
}
