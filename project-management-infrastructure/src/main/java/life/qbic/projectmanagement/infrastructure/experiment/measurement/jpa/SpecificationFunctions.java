package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;

public interface SpecificationFunctions {

  String CUSTOM_DATE_TIME_PATTERN = "%Y-%m-%d %H:%i";

  /**
   * Turns milliseconds into an offset String
   *
   * @param offsetMillis
   * @return
   */
  static String formatMillisToOffsetString(int offsetMillis) {
    var prefix = offsetMillis >= 0 ? "+" : "";
    var separator = ":";
    var seconds = offsetMillis / 1000;
    int minutes = Math.divideExact(seconds, 60);
    int hours = Math.divideExact(minutes, 60);
    minutes = minutes - hours * 60;
    DecimalFormat decimalFormat = new DecimalFormat("##00");
    return prefix + decimalFormat.format(hours) + separator + decimalFormat.format(minutes);
  }

  static Expression<LocalDateTime> getConvertTz(CriteriaBuilder criteriaBuilder,
      Path<Instant> instantPath, String clientTimeOffset) {
    return criteriaBuilder.function("CONVERT_TZ",
        LocalDateTime.class,
        instantPath,
        //always +00:00 as java.time.Instant is always UTC
        criteriaBuilder.literal("+00:00"),
        criteriaBuilder.literal(clientTimeOffset));
  }

  static <X> Expression<String> extractFormattedLocalDate(CriteriaBuilder criteriaBuilder,
      Path<Instant> property, int clientTimeOffsetMillis, String dateTimePattern) {
    var clientTimeOffset = formatMillisToOffsetString(clientTimeOffsetMillis);
    //for CONVERT_TZ see https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/convert_tz
    var registrationAtClientTimeZone =
        SpecificationFunctions.getConvertTz(criteriaBuilder, property, clientTimeOffset);
    //for DATA_FORMAT see https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/date_format
    return criteriaBuilder.function("DATE_FORMAT",
        String.class,
        registrationAtClientTimeZone,
        criteriaBuilder.literal(dateTimePattern));
  }

  static Predicate containsString(CriteriaBuilder criteriaBuilder, Path<String> property,
      String searchTerm) {
    return criteriaBuilder.like(
        criteriaBuilder.lower(property),
        "%" + searchTerm.strip().toLowerCase() + "%");
  }

  static Predicate containsString(CriteriaBuilder criteriaBuilder,
      Expression<String> stringExpression,
      String searchTerm) {
    return criteriaBuilder.like(
        criteriaBuilder.lower(stringExpression),
        "%" + searchTerm.strip().toLowerCase() + "%");
  }
}
