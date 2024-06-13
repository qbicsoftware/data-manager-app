package life.qbic.projectmanagement.application;

import static java.util.Objects.isNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import life.qbic.logging.api.Logger;
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

  private static final Logger log = logger(CollaboratorUserInfosConverter.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<UserInfo> attribute) {
    if (isNull(attribute)) {
      return null;
    }
    if (attribute.isEmpty()) {
      return null;
    }
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      objectMapper.writeValue(outputStream, attribute);
      return outputStream.toString();
    } catch (IOException e) {
      log.error("Unexpected problems writing project collaborators to the database", e);
      return null;
    }
  }

  @Override
  public List<UserInfo> convertToEntityAttribute(String dbData) {
    if (isNull(dbData)) {
      return new ArrayList<>();
    }
    try {
      return objectMapper.readValue(dbData,
          objectMapper.getTypeFactory().constructCollectionType(
              List.class, UserInfo.class)
      );
    } catch (JsonProcessingException e) {
      log.error("Unexpected failure parsing project collaborators from database", e);
      return new ArrayList<>();
    }

  }
}
