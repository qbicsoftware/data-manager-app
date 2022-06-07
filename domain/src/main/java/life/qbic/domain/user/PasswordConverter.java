package life.qbic.domain.user;

import javax.persistence.AttributeConverter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class PasswordConverter implements AttributeConverter<EncryptedPassword, String> {

  @Override
  public String convertToDatabaseColumn(EncryptedPassword email) {
    return email.hash();
  }

  @Override
  public EncryptedPassword convertToEntityAttribute(String s) {
    return EncryptedPassword.fromEncrypted(s);
  }
}
