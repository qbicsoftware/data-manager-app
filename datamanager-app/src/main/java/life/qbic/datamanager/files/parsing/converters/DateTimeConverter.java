package life.qbic.datamanager.files.parsing.converters;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import life.qbic.application.commons.time.DateTimeFormat;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class DateTimeConverter {

  private DateTimeConverter() {

  }

  /**
   * Parses a textual date or date-time value into a UTC {@link Instant} using the canonical pattern
   * defined by the given {@link DateTimeFormat}.
   *
   * <p>Date-only formats ({@link DateTimeFormat#ISO_LOCAL_DATE},
   * {@link DateTimeFormat#SIMPLE_DATE}) are resolved to UTC midnight of the parsed date. Date-time
   * formats are interpreted as UTC.
   *
   * <p>Returns {@code null} if the value is blank or cannot be parsed according to the
   * given format, allowing callers such as validation services to handle missing or malformed
   * values as constraint violations rather than exceptions.
   *
   * @param dateTimeFormat the expected format of the input value; must not be {@code null}
   * @param value          the raw string to parse; may be {@code null} or blank
   * @return the parsed {@link Instant} at UTC, or {@code null} if the value is blank or unparseable
   */
  @Nullable
  public static Instant parseInstant(@NonNull DateTimeFormat dateTimeFormat,
      @Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    var formatter = DateTimeFormat.asJavaFormatter(dateTimeFormat, ZoneOffset.UTC);
    try {
      return switch (dateTimeFormat) {
        case ISO_LOCAL_DATE, SIMPLE_DATE -> LocalDate.parse(value.strip(), formatter)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();
        case ISO_LOCAL_DATE_TIME, ISO_LOCAL_DATE_TIME_WHITESPACE_SEPARATED, SIMPLE_DATE_TIME ->
            LocalDateTime.parse(value.strip(), formatter)
                .toInstant(ZoneOffset.UTC);
      };
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
