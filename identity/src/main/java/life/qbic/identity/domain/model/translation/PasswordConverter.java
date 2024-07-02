package life.qbic.identity.domain.model.translation;

import static java.util.Objects.isNull;

import jakarta.persistence.AttributeConverter;
import life.qbic.identity.domain.model.EncryptedPassword;

/**
 * <b>Converts {@link EncryptedPassword} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link EncryptedPassword}. Additionally converts the {@link EncryptedPassword} to a string value
 * to be stored in the database.
 * </p>
 *
 * @since 1.0.0
 */
public class PasswordConverter implements AttributeConverter<EncryptedPassword, String> {

  @Override
  public String convertToDatabaseColumn(EncryptedPassword password) {
    if (isNull(password)) {
      return null;
    }
    return password.get();
  }

  @Override
  public EncryptedPassword convertToEntityAttribute(String s) {
    if (isNull(s)) {
      return null;
    }
    return EncryptedPassword.fromEncrypted(s);
  }
}
