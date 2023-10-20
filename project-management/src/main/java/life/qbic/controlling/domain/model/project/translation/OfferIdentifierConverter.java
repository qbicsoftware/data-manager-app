package life.qbic.controlling.domain.model.project.translation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.controlling.domain.model.project.OfferIdentifier;

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
