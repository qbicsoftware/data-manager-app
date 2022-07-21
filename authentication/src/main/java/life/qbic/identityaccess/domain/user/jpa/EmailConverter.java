package life.qbic.identityaccess.domain.user.jpa;

import javax.persistence.AttributeConverter;
import life.qbic.identityaccess.domain.user.EmailAddress;

/**
 * <b>Converts {@link EmailAddress} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link EmailAddress}. Additionally converts the
 * {@link EmailAddress} to a string value to be stored in the database.
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
