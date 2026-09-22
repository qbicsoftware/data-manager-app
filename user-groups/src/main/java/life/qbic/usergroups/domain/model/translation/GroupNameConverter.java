package life.qbic.usergroups.domain.model.translation;

import jakarta.persistence.AttributeConverter;
import life.qbic.usergroups.domain.model.GroupName;

/**
 * <b>Converts {@link GroupName} into a String and vice versa</b>
 *
 * <p>Converts the String value stored in the database to a {@link GroupName}. Additionally
 * converts the {@link GroupName} to a string value to be stored in the database. Mirrors the
 * identity context's {@code FullNameConverter}.
 * </p>
 *
 * @since 1.19.0
 */
public class GroupNameConverter implements AttributeConverter<GroupName, String> {

  @Override
  public String convertToDatabaseColumn(GroupName groupName) {
    return groupName == null ? null : groupName.value();
  }

  @Override
  public GroupName convertToEntityAttribute(String s) {
    return s == null ? null : GroupName.from(s);
  }
}