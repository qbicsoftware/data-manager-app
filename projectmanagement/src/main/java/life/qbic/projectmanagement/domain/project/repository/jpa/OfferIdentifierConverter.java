package life.qbic.projectmanagement.domain.project.repository.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
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
