package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.AttributeConverter;

/**
 * <b>NGSMeasurementMetadata Code</b>
 *
 * <p>A measurement code is s short label for measurement entities to help the user associate it
 * with a sample</p>
 * <p>
 * A measurement code starts with a domain prefix, like "NGS" for Next Generation Sequencing or "MS"
 * for Mass Spectrometry, "IMG" for imaging and so on.
 * <p>
 * The prefix is followed by an incremented number, indicating that there might be more than one
 * measurement associated to the sample.
 * <p>
 * Finally, the measurement code is completed with the sample code it is referring to.
 * <p>
 * The following examples are valid measurement codes:
 *
 * <ul>
 *   <li>NGS1QTEST001AE</li>
 *   <li>MS2QTEST001AE</li>
 *   <li>IMG10TEST001AE</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class MeasurementCode {

  private final MEASUREMENT_PREFIX prefix;
  private final String measurementCode;

  protected MeasurementCode() {
    measurementCode = null;
    prefix = null;
  }

  private MeasurementCode(MEASUREMENT_PREFIX prefix, String sampleCode, Long nanoTimeStamp) {
    this.prefix = prefix;
    this.measurementCode = "%s-%s".formatted((prefix + sampleCode), nanoTimeStamp);
  }

  private static MeasurementCode create(MEASUREMENT_PREFIX prefix, String sampleCode) {
    if (sampleCode.isBlank()) {
      throw new IllegalArgumentException("Sample code must not be blank or empty.");
    }
    return new MeasurementCode(prefix, sampleCode, System.nanoTime());
  }

  public static MeasurementCode createNGS(String sampleCode) {
    return MeasurementCode.create(MEASUREMENT_PREFIX.NGS, sampleCode);
  }

  public static MeasurementCode createMS(String sampleCode) {
    return MeasurementCode.create(MEASUREMENT_PREFIX.MS, sampleCode);
  }

  public static MeasurementCode parse(String value) {
    for (MEASUREMENT_PREFIX prefix : MEASUREMENT_PREFIX.values()) {
      if (value.startsWith(prefix.toString())) {
        try {
          return new MeasurementCode(prefix,
              value.split("-")[0].substring(prefix.toString().length()),
              Long.parseLong(value.split("-")[1]));
        } catch (RuntimeException e) {
          throw new IllegalArgumentException(
              "Unknown value for a measurement code for: \"" + value + "\"");
        }
      }
    }
    throw new IllegalArgumentException("Unknown measurement code prefix for: \"" + value + "\"");
  }

  public String value() {
    return this.measurementCode;
  }

  public boolean isNGSDomain() {
    return prefix == MEASUREMENT_PREFIX.NGS;
  }

  public boolean isMSDomain() {
    return prefix == MEASUREMENT_PREFIX.MS;
  }

  public boolean isIMGDomain() {
    return prefix == MEASUREMENT_PREFIX.IMG;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeasurementCode that)) {
      return false;
    }

    if (prefix != that.prefix) {
      return false;
    }
    return measurementCode.equals(that.measurementCode);
  }

  @Override
  public int hashCode() {
    int result = prefix.hashCode();
    result = 31 * result + measurementCode.hashCode();
    return result;
  }

  public enum MEASUREMENT_PREFIX {
    NGS, MS, IMG
  }
  static class MeasurementCodeConverter implements AttributeConverter<MeasurementCode, String> {

    @Override
    public String convertToDatabaseColumn(MeasurementCode measurementCode) {
      return measurementCode.value();
    }

    @Override
    public MeasurementCode convertToEntityAttribute(String s) {
      return MeasurementCode.parse(s);
    }
  }


}
