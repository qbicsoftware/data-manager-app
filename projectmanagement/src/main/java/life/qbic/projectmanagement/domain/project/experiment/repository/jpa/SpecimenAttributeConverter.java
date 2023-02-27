package life.qbic.projectmanagement.domain.project.experiment.repository.jpa;

import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
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
