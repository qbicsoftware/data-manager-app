package life.qbic.projectmanagement.application.sample;

/**
 * <b>Field lengths of the sample concern</b>
 *
 * <p>The maximum lengths of user-editable sample free-text fields, matching their storage columns.
 * Kept separate from the measurement limits so the two concerns can change independently.
 *
 * <p>See #1695: these columns are {@code varchar(255)}, while the templates advertised 500
 * characters and no layer validated the length.
 *
 * @since 1.20.3
 */
public final class SampleFieldLength {

  /**
   * Maximum length of {@code sample.comment}, a {@code varchar(255)} column.
   */
  public static final int COMMENT = 255;

  /**
   * Maximum length of {@code sample.batch}, a {@code varchar(255)} column.
   */
  public static final int BATCH = 255;

  private SampleFieldLength() {
    // constants only
  }
}
