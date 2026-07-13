package life.qbic.projectmanagement.infrastructure.dataset;

import static life.qbic.logging.service.LoggerFactory.logger;

import tools.jackson.core.JsonProcessingException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import java.io.IOException;
import java.util.List;
import life.qbic.logging.api.Logger;

/**
 * <b>Measured Samples Converter</b>
 * <p>
 * Converter that enables database JSON array and {@link List} of {@link MeasuredSample}
 * conversion.
 *
 * @since 1.12.0
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
