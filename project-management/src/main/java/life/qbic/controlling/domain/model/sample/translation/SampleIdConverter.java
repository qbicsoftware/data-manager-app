package life.qbic.controlling.domain.model.sample.translation;

import jakarta.persistence.AttributeConverter;
import life.qbic.controlling.domain.model.sample.SampleId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SampleIdConverter implements AttributeConverter<SampleId, String> {

  @Override
  public String convertToDatabaseColumn(SampleId sampleId) {
    return sampleId.value();
  }

  @Override
  public SampleId convertToEntityAttribute(String s) {
    return SampleId.parse(s);
  }
}
