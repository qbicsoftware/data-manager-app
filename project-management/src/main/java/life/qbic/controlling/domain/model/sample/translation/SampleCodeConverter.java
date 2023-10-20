package life.qbic.controlling.domain.model.sample.translation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.controlling.domain.model.sample.SampleCode;

/**
 * <b>Converts {@link SampleCode} into a String and vice versa></b>
 *
 * <p>Converts the String value stored in the database to an
 * {@link SampleCode}. Additionally converts the {@link SampleCode} to a string value to be stored
 * in the database.
 * </p>
 */
@Converter(autoApply = true)
public class SampleCodeConverter implements AttributeConverter<SampleCode, String> {

  @Override
  public String convertToDatabaseColumn(SampleCode sampleCode) {
    return sampleCode.code();
  }

  @Override
  public SampleCode convertToEntityAttribute(String s) {
    return SampleCode.create(s);
  }
}
