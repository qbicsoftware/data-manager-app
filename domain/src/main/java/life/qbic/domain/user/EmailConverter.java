package life.qbic.domain.user;

import javax.persistence.AttributeConverter;

/**
 * <b>Converts {@link life.qbic.domain.user.EmailAddress} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link life.qbic.domain.user.EmailAddress}. Additionally converts the
 * {@link life.qbic.domain.user.EmailAddress} to a string value to be stored in the database.
 * </p>
 *
 * @since 1.0.0
 */
public class EmailConverter implements AttributeConverter<EmailAddress, String> {

  @Override
  public String convertToDatabaseColumn(EmailAddress emailAddress) {
    return emailAddress.get();
  }

  @Override
  public EmailAddress convertToEntityAttribute(String s) {
    return EmailAddress.from(s);
  }
}
