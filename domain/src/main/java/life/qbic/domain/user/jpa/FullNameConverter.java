package life.qbic.domain.user.jpa;

import javax.persistence.AttributeConverter;
import life.qbic.domain.user.FullName;

/**
 * <b>Converts {@link life.qbic.domain.user.FullName} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link life.qbic.domain.user.FullName}. Additionally converts the
 * {@link life.qbic.domain.user.FullName} to a string value to be stored in the database.
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
