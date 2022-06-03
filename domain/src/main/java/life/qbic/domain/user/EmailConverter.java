package life.qbic.domain.user;

import javax.persistence.AttributeConverter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EmailConverter implements AttributeConverter<Email, String> {

  @Override
  public String convertToDatabaseColumn(Email email) {
    return email.address();
  }

  @Override
  public Email convertToEntityAttribute(String s) {
    return Email.from(s);
  }
}
