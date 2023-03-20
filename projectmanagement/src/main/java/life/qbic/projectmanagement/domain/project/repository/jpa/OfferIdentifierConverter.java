package life.qbic.projectmanagement.domain.project.repository.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;

/**
 * Converts offer identifiers to string for storage in persistence.
 */
@Converter(autoApply = true)
public class OfferIdentifierConverter implements AttributeConverter<OfferIdentifier, String> {

  @Override
  public String convertToDatabaseColumn(OfferIdentifier attribute) {
    return attribute.value();
  }

  @Override
  public OfferIdentifier convertToEntityAttribute(String dbData) {
    return OfferIdentifier.of(dbData);
  }
}
