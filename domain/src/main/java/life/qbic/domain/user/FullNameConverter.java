package life.qbic.domain.user;

import javax.persistence.AttributeConverter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FullNameConverter implements AttributeConverter<FullName, String> {

  @Override
  public String convertToDatabaseColumn(FullName fullName) {
    return fullName.name();
  }

  @Override
  public FullName convertToEntityAttribute(String s) {
    return FullName.from(s);
  }
}
