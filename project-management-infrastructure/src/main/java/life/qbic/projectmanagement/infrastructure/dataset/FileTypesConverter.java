package life.qbic.projectmanagement.infrastructure.dataset;

import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import life.qbic.logging.api.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


@Converter(autoApply = false)
class FileTypesConverter implements AttributeConverter<Set<String>, String> {

  private static final Logger log = logger(FileTypesConverter.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Set<String> s) {
    if (s == null || s.isEmpty()) return "[]";
    try {
      return objectMapper.writeValueAsString(s);
    } catch (JacksonException e) {
      log.error(e.getMessage(), e);
      throw new IllegalArgumentException("Cannot convert " + s + " to JSON");
    }
  }

  @Override
  public Set<String> convertToEntityAttribute(String s) {
    if (s == null || s.isEmpty()) return new HashSet<>();
    try {
      return new LinkedHashSet<>(Arrays.asList(objectMapper.readValue(s, String[].class)));
    } catch (JacksonException e) {
      log.error(e.getMessage(), e);
      throw new IllegalArgumentException("Cannot convert " + s + " from JSON");
    }
  }
}
