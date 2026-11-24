package life.qbic.projectmanagement.application.measurement.validation;

/**
 * <b>Field lengths of the measurement concern</b>
 *
 * <p>The maximum length of the measurement comment, per measurement type. Each type has its own
 * column and can change independently, also from the sample limits in
 * {@code SampleFieldLength}.
 *
 * <p>See #1706: these columns are {@code varchar(255)}, while the templates advertised 500
 * characters and no layer validated the length.
 *
 * @since 1.20.3
 */
public final class MeasurementFieldLength {

  /**
   * Maximum length of {@code specific_measurement_metadata_ngs.comment}.
   */
  public static final int NGS_COMMENT = 255;

  /**
   * Maximum length of {@code specific_measurement_metadata_pxp.comment}.
   */
  public static final int PROTEOMICS_COMMENT = 255;

  /**
   * Maximum length of {@code ip_measurements.comment}.
   */
  public static final int IMMUNOPEPTIDOMICS_COMMENT = 255;

  private MeasurementFieldLength() {
    // constants only
  }
}
