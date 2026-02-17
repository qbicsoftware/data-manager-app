package life.qbic.application.commons.time;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.lang.NonNull;

/**
 * Defines canonical date/time formats used consistently across the application.
 *
 * <p>This type acts as a cross-layer contract between:
 * <ul>
 *   <li>UI rendering (via {@link #asJavaFormatter(DateTimeFormat, ZoneId)})</li>
 *   <li>Database-side filtering (via {@link #asMariaDbDatabasePattern(DateTimeFormat)})</li>
 * </ul>
 *
 * <p>The goal is to ensure that textual date representations shown to the user
 * exactly match the representations used for textual filtering in the database,
 * allowing users to search for values as displayed.
 *
 * <p>Each format therefore provides both:
 * <ul>
 *   <li>A {@link DateTimeFormatter} correctly configured with the corresponding pattern</li>
 *   <li>A MariaDB <a href="https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/date_format">{@code DATE_FORMAT}</a> pattern to be used with correctly zoned dates</li>
 * </ul>
 */

public enum DateTimeFormat {


  /**
   * A simple to read, textual date representation. E.g. {@code Wednesday, 11 February 2026}
   * <p>
   * Format: {@code <weekday_name>, <day> <month_name> <year>}}
   */
  SIMPLE_DATE,
  /**
   * A simple to read, textual date-time representation. E.g.
   * {@code Wednesday, 11 February 2026 11:12:10}
   * <p>
   * Format: {@code <weekday_name>, <day> <month_name> <year>
   * <hour>:<minute>:<seconds>}}
   */
  SIMPLE_DATE_TIME,
  /**
   * The ISO-8601 date format without an offset, such as '2011-12-03'.
   * <p>
   * Format: {@code <year>-<month>-<day>}
   */
  ISO_LOCAL_DATE,
  /**
   * The ISO-8601 date-time format without an offset, such as '2011-12-03T10:15:30'.
   * <p>
   * Format: {@code <year>-<month>-<day>T<hour>:<minute>:<second>}
   */
  ISO_LOCAL_DATE_TIME,
  /**
   * The ISO-8601 date-time format without an offset. Date and time are separated by a space instead of T, such as '2011-12-03 10:15:30'.
   * <p>
   * Format: {@code <year>-<month>-<day> <hour>:<minute>:<second>}
   */
  ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED;

  /**
   * Creates a {@link DateTimeFormatter} for rendering Instants in the UI.
   *
   * <p>The formatter must produce a textual representation equivalent to thee
   *    * MariaDB {@code DATE_FORMAT} pattern returned by {@link #asMariaDbDatabasePattern(DateTimeFormat)},
   * so that user-entered search terms match the displayed values.
   *
   * @return formatter matching the database-side date representation
   */

  @NonNull
  public static DateTimeFormatter asJavaFormatter(@NonNull DateTimeFormat format, ZoneId zoneId) {
    return switch (format) {
      case ISO_LOCAL_DATE -> DateTimeFormatter.ISO_LOCAL_DATE.withZone(zoneId);
      case ISO_LOCAL_DATE_TIME -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zoneId);
      case ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED -> DateTimeFormatter.ofPattern(
          "yyyy-MM-dd HH:mm:ss").withZone(zoneId);
      case SIMPLE_DATE -> DateTimeFormatter.ofPattern("EEEE, dd LLLL yyyy").withZone(zoneId);
      case SIMPLE_DATE_TIME -> DateTimeFormatter.ofPattern("EEEE, dd LLLL yyyy HH:mm:ss").withZone(
          zoneId);
    };
  }

  /**
   * Returns the MariaDB <a href="https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/date_format">{@code DATE_FORMAT}</a> pattern corresponding to the UI format.
   *
   * <p>This pattern is used in JPA Specifications to transform Instants into
   * textual representations that align with the UI rendering format.
   *
   * @return MariaDB date format pattern
   */
  @NonNull
  public static String asMariaDbDatabasePattern(@NonNull DateTimeFormat format) {
    return switch (format) {
      case SIMPLE_DATE -> "%W, %d %M %Y";
      case SIMPLE_DATE_TIME -> "%W, %d %M %Y %T";
      case ISO_LOCAL_DATE -> "%Y-%m-%d";
      case ISO_LOCAL_DATE_TIME -> "%Y-%m-%dT%T";
      case ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED -> "%Y-%m-%d %T";
    };
  }
}
