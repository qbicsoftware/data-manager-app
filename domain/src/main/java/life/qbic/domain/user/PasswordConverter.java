package life.qbic.domain.user;

import javax.persistence.AttributeConverter;

/**
 * <b>Converts {@link life.qbic.domain.user.EncryptedPassword} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link life.qbic.domain.user.EncryptedPassword}. Additionally converts the
 * {@link life.qbic.domain.user.EncryptedPassword} to a string value to be stored in the database.
 * </p>
 *
 * @since 1.0.0
 */
public class PasswordConverter implements AttributeConverter<EncryptedPassword, String> {

  @Override
  public String convertToDatabaseColumn(EncryptedPassword password) {
    return password.value();
  }

  @Override
  public EncryptedPassword convertToEntityAttribute(String s) {
    return EncryptedPassword.fromEncrypted(s);
  }
}
