package life.qbic.projectmanagement.infrastructure.dataset;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import java.util.List;
import life.qbic.logging.api.Logger;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasuredSamplesConverter implements
    AttributeConverter<List<MeasuredSample>, String> {

  private static final Logger log = logger(MeasuredSamplesConverter.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<MeasuredSample> measuredSamples) {
    try {
      return MAPPER.writeValueAsString(measuredSamples);
    } catch (JsonProcessingException e) {
      log.error("Error converting measured samples to json", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public List<MeasuredSample> convertToEntityAttribute(String s) {
    try {
      return MAPPER.readValue(s, new TypeReference<List<MeasuredSample>>() {
      });
    } catch (JsonProcessingException e) {
      log.error("Error converting json to measured samples", e);
      throw new IllegalStateException(e);
    }
  }
}
