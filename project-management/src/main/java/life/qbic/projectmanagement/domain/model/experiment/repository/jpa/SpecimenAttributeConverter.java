package life.qbic.projectmanagement.domain.model.experiment.repository.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;

@Converter(autoApply = true)
public class SpecimenAttributeConverter implements AttributeConverter<Specimen, String> {

  @Override
  public String convertToDatabaseColumn(Specimen attribute) {
    return attribute.label();
  }

  @Override
  public Specimen convertToEntityAttribute(String dbData) {
    return Specimen.create(dbData);
  }
}
