package life.qbic.datamanager.views.general;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * <b>Date Time Rendering</b>
 *
 * <p>A collection of utility methods to render instants in a harmonised way throughout the
 * application.</p>
 *
 * @since 1.7.0
 */
public class DateTimeRendering {

  private static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm";

  /**
   * Formats an {@link Instant} in "dd.MM.yyyy HH:mm".
   *
   * @param instant the instant to format
   * @return the formatted instant
   * @since 1.7.0
   */
  public static String simple(Instant instant) {
    var formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.systemDefault());
    return formatter.format(instant);
  }

}
