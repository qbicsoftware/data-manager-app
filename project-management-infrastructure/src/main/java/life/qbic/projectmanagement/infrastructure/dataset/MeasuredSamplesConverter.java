package life.qbic.projectmanagement.infrastructure.dataset;

import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import life.qbic.logging.api.Logger;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * <b>Measured Samples Converter</b>
 * <p>
 * Converter that enables database JSON array and {@link List} of {@link MeasuredSample}
 * conversion.
 *
 * @since 1.12.0
 */
@Converter(autoApply = false)
public class MeasuredSamplesConverter implements
    AttributeConverter<List<MeasuredSample>, String> {

  private static final Logger log = logger(MeasuredSamplesConverter.class);
  private final ObjectMapper mapper;

  public MeasuredSamplesConverter(@NonNull ObjectMapper mapper) {
    this.mapper = mapper;
  }
  @Override
  public String convertToDatabaseColumn(List<MeasuredSample> measuredSamples) {
    try {
      return mapper.writeValueAsString(measuredSamples);
    } catch (JacksonException e) {
      log.error("Error converting measured samples to json", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public List<MeasuredSample> convertToEntityAttribute(String s) {
    try {
      return mapper.readValue(s, new TypeReference<List<MeasuredSample>>() {
      });
    } catch (JacksonException e) {
      log.error("Error converting json to measured samples", e);
      throw new IllegalStateException(e);
    }
  }
}
