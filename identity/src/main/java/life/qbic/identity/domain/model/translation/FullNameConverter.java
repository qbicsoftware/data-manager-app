package life.qbic.identity.domain.model.translation;

import jakarta.persistence.AttributeConverter;
import life.qbic.identity.domain.model.FullName;

/**
 * <b>Converts {@link FullName} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link FullName}. Additionally converts the {@link FullName} to a string value to be stored in
 * the database.
 * </p>
 *
 * @since 1.0.0
 */
public class FullNameConverter implements AttributeConverter<FullName, String> {

  @Override
  public String convertToDatabaseColumn(FullName fullName) {
    return fullName.get();
  }

  @Override
  public FullName convertToEntityAttribute(String s) {
    return FullName.from(s);
  }
}
