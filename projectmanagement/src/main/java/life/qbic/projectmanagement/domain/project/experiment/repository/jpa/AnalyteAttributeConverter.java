package life.qbic.projectmanagement.domain.project.experiment.repository.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;

@Converter(autoApply = true)
public class AnalyteAttributeConverter implements AttributeConverter<Analyte, String> {

  @Override
  public String convertToDatabaseColumn(Analyte attribute) {
    return attribute.label();
  }

  @Override
  public Analyte convertToEntityAttribute(String dbData) {
    return Analyte.create(dbData);
  }
}
