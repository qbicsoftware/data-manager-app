package life.qbic.projectmanagement.domain.model.measurement;

/**
 * <b>Measurement Code</b>
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
  private final int counter;
  private final String sampleCode;
  private final String measurementCode;

  private MeasurementCode(MEASUREMENT_PREFIX prefix, int measurementCounter, String sampleCode) {
    this.prefix = prefix;
    this.counter = measurementCounter;
    this.sampleCode = sampleCode;
    this.measurementCode = String.valueOf(prefix) + measurementCounter
        + sampleCode;
  }

  public static MeasurementCode create(MEASUREMENT_PREFIX prefix, int measurementCounter,
      String sampleCode) {
    if (measurementCounter <= 0) {
      throw new IllegalArgumentException(
          "Measurement counter must be greater zero. Provided value was " + measurementCounter);
    }
    if (sampleCode.isBlank()) {
      throw new IllegalArgumentException("Sample code must not be blank or empty.");
    }
    return new MeasurementCode(prefix, measurementCounter, sampleCode);
  }


  public String value() {
    return this.measurementCode;
  }

  public static MeasurementCode parse(String value) {
    for (MEASUREMENT_PREFIX prefix : MEASUREMENT_PREFIX.values()) {
      if (value.startsWith(prefix.toString())) {
        try {
          return new MeasurementCode(prefix,
              Integer.parseInt(value.substring(prefix.toString().length(), value.length() - 10)),
              value.substring(value.length() - 10));
        } catch (Exception e) {
          throw new IllegalArgumentException("Unknown value for a measurement code for: \"" + value + "\"");
        }
      }
    }
    throw new IllegalArgumentException("Unknown measurement code prefix for: \"" + value + "\"");
  }

  public enum MEASUREMENT_PREFIX {
    NGS, MS, IMG
  }


}
