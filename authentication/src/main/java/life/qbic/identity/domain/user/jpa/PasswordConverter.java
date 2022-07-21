package life.qbic.identity.domain.user.jpa;

import javax.persistence.AttributeConverter;
import life.qbic.identity.domain.user.EncryptedPassword;

/**
 * <b>Converts {@link EncryptedPassword} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link EncryptedPassword}. Additionally converts the
 * {@link EncryptedPassword} to a string value to be stored in the database.
 * </p>
 *
 * @since 1.0.0
 */
public class PasswordConverter implements AttributeConverter<EncryptedPassword, String> {

  @Override
  public String convertToDatabaseColumn(EncryptedPassword password) {
    return password.get();
  }

  @Override
  public EncryptedPassword convertToEntityAttribute(String s) {
    return EncryptedPassword.fromEncrypted(s);
  }
}
