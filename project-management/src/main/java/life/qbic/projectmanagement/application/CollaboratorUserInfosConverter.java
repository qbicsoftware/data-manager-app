package life.qbic.projectmanagement.application;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;

/**
 * Converts a string containing a json list of user info json objects to the java equivalent and
 * vice versa.
 *
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class CollaboratorUserInfosConverter implements
    AttributeConverter<List<UserInfo>, String> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<UserInfo> attribute) {
    if (isNull(attribute)) {
      return null;
    }
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      objectMapper.writeValue(outputStream, attribute);
      return outputStream.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<UserInfo> convertToEntityAttribute(String dbData) {
    if (isNull(dbData)) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData,
          objectMapper.getTypeFactory().constructCollectionType(
              List.class, UserInfo.class)
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

  }
}
