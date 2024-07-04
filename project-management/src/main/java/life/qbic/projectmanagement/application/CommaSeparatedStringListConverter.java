package life.qbic.projectmanagement.application;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts a string of comma separated usernames to a list of usernames.
 *
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class CommaSeparatedStringListConverter implements
    AttributeConverter<List<String>, String> {

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    return String.join(", ", attribute);
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return new ArrayList<>();
    } else {
      return Arrays.stream(dbData.split(","))
          .map(String::strip)
          .toList();
    }
  }
}
