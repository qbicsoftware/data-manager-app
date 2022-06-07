package life.qbic.domain.user;

import javax.persistence.AttributeConverter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EmailConverter implements AttributeConverter<EmailAddress, String> {

  @Override
  public String convertToDatabaseColumn(EmailAddress emailAddress) {
    return emailAddress.address();
  }

  @Override
  public EmailAddress convertToEntityAttribute(String s) {
    return EmailAddress.from(s);
  }
}
