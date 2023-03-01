package life.qbic.projectmanagement.domain.project.experiment.repository.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;

@Converter(autoApply = true)
public class SpeciesAttributeConverter implements AttributeConverter<Species, String> {

  @Override
  public String convertToDatabaseColumn(Species attribute) {
    return attribute.label();
  }

  @Override
  public Species convertToEntityAttribute(String dbData) {
    return Species.create(dbData);
  }
}
