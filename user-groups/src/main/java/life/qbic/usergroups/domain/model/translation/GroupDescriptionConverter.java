package life.qbic.usergroups.domain.model.translation;

import jakarta.persistence.AttributeConverter;
import life.qbic.usergroups.domain.model.GroupDescription;

/**
 * <b>Converts {@link GroupDescription} into a String and vice versa</b>
 *
 * <p>Converts the String value stored in the database to a {@link GroupDescription}. Additionally
 * converts the {@link GroupDescription} to a string value to be stored in the database. Null maps
 * to {@code null} (the description is optional).
 * </p>
 *
 * @since 1.19.0
 */
public class GroupDescriptionConverter implements AttributeConverter<GroupDescription, String> {

  @Override
  public String convertToDatabaseColumn(GroupDescription groupDescription) {
    if (groupDescription == null) {
      return null;
    }
    return groupDescription.value().orElse(null);
  }

  @Override
  public GroupDescription convertToEntityAttribute(String s) {
    return s == null ? null : GroupDescription.from(s);
  }
}