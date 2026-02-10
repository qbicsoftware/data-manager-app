package life.qbic.application.commons.time;

import java.time.format.DateTimeFormatter;
import org.springframework.lang.NonNull;

public enum DateTimeFormat {

  SIMPLE,
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED;

  @NonNull
  public static DateTimeFormatter asJavaFormatter(DateTimeFormat format) {
    return switch (format) {
      case ISO_LOCAL_DATE -> DateTimeFormatter.ISO_LOCAL_DATE;
      case ISO_LOCAL_DATE_TIME -> DateTimeFormatter.ISO_LOCAL_DATE_TIME;
      case ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED -> DateTimeFormatter.ofPattern(
          "yyyy-MM-dd HH:mm");
      case SIMPLE -> DateTimeFormatter.ofPattern("EEEE, dd LLLL yyyy HH:mm:ss");
    };
  }

  @NonNull
  public static String asDatabasePattern(DateTimeFormat format) {
    return switch (format) {
      case SIMPLE -> "%W, %d %M %Y %T";
      case ISO_LOCAL_DATE -> "%Y-%m-%d";
      case ISO_LOCAL_DATE_TIME -> "%Y-%m-%dT%T";
      case ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED -> "%Y-%m-%d %T";
    };
  }
}
