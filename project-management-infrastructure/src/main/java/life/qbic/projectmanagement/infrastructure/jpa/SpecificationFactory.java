package life.qbic.projectmanagement.infrastructure.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.Function;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

/**
 * A factory for {@link Specification} based on commonly required parameters.
 */
public class SpecificationFactory {

  private static final Logger log = LoggerFactory.logger(SpecificationFactory.class);
  /**
   * A custom date time format not in any java.time ISO formats
   */
  public static final String CUSTOM_DATE_TIME_PATTERN = "%Y-%m-%d %H:%i";


  /**
   * Extracts an {@link Expression} of type {@link T }from another {@link Expression}  of type {@link S}
   * @param <S> The type of item used in the expression
   * @param <T> the desired type of expression after extraction
   * @param <X> the concrete type of source Expression
   */
  public interface Extractor<S, T, X extends Expression<S>> extends Function<X, Expression<T>> {

    @Override
    default Expression<T> apply(X expression) {
      return extractFrom(expression);
    }

    /**
     * Extract an expression from the provided expression.
     *
     * @param expression the provided expression
     * @return the extracted expression
     */
    Expression<T> extractFrom(X expression);

  }

  /**
   * Creates a {@link Specification} that passes when a property of the provided name contains the search term.
   * @param propertyName the property of the {@link Root} of the {@link Specification}
   * @param searchTerm the term to search for
   * @return a configured Specification
   * @param <T> the type of {@link Root}
   */
  public static <T> Specification<T> propertyContains(String propertyName, String searchTerm) {
    return (root, query, criteriaBuilder) -> {
      Class<?> propertyType = root.get(propertyName).getJavaType();
      if (propertyType != String.class) {
        log.debug(
            "Property %s is not of type String.class but is %s. Trying to cast.".formatted(
                propertyName, propertyType));
        return contains(criteriaBuilder, root.get(propertyName).as(String.class), searchTerm);
      }
      return contains(criteriaBuilder, root.get(propertyName), searchTerm);
    };
  }

  /**
   * Creates a {@link Specification} that passes when value of an {@link Expression} contains the search term.
   * @param extractor an {@link Extractor} that provides an {@link Expression} of type {@link String}
   * @param searchTerm the term to search for
   * @return a configured Specification
   * @param <T> the type of the {@link Specification} {@link Root}
   */
  public static <T> Specification<T> contains(
      Extractor<T, String, Root<T>> extractor,
      String searchTerm) {
    return (root, query, criteriaBuilder) ->
        contains(criteriaBuilder, extractor.extractFrom(root), searchTerm);
  }

  /**
   * Creates a {@link Specification} that passes when value of an {@link Expression} equals the provided value.
   * @param extractor an {@link Extractor} that provides an {@link Expression} of type {@link S}
   * @param other the object to compare to
   * @return a configured Specification
   * @param <S> the type of item that is compared for equality
   * @param <T> the type of the {@link Specification} {@link Root}
   */
  public static <S, T> Specification<T> exactMatches(
      Extractor<T, S, Root<T>> extractor,
      S other
  ) {
    return (root, query, criteriaBuilder) ->
        objectEquals(criteriaBuilder, extractor.extractFrom(root), other);
  }

  /**
   * Creates a {@link Specification} that passes when the formatted value of an {@link Expression} contains the search term.
   * @param instantPropertyName the name of the property of type {@link Instant}
   * @param searchTerm the term to search for
   * @param clientOffsetMillis the millisecond offset used to deduct the client time zone
   * @param dateTimePattern the format pattern to format the time. The formatted time is searched for containing the search term.
   * @return a configured Specification
   * @param <T> the type of the {@link Specification} {@link Root}
   */
  public static <T> Specification<T> formattedClientTimeContains(String instantPropertyName,
      String searchTerm, int clientOffsetMillis, String dateTimePattern) {
    return (root, query, criteriaBuilder) -> {
      Class<?> propertyType = root.get(instantPropertyName).getJavaType();
      if (!propertyType.isAssignableFrom(Instant.class)) {
        log.error("Expected property of type %s but got %s".formatted(Instant.class, propertyType));
        return criteriaBuilder.disjunction();
      }
      return contains(criteriaBuilder,
          extractFormattedLocalDate(criteriaBuilder, root.get(instantPropertyName),
              clientOffsetMillis, dateTimePattern),
          searchTerm);
    };
  }

  /**
   * Creates a {@link Specification} that passes when an {@link Expression} resulting in a JSON contains the search term at the specified JSON path.
   * @param extractor an {@link Extractor} that provides an {@link Expression} of type {@link S}
   * @param jsonPath json path to be searched
   * @param searchTerm the term to search for
   * @return a configured Specification
   * @param <S> the type of item that is compared for equality
   * @param <T> the type of the {@link Specification} {@link Root}
   * @see <a href="https://mariadb.com/docs/server/reference/sql-functions/special-functions/json-functions/jsonpath-expressions">JSONPath Expressions</a>
   */
  public static <S, T> Specification<S> jsonContains(Extractor<S, T, Root<S>> extractor,
      String jsonPath, String searchTerm) {
    return (root, query, criteriaBuilder) ->
        jsonContains(criteriaBuilder, extractor.extractFrom(root), jsonPath, searchTerm);

  }

  /**
   * Wraps a {@link Specification} in a distinct constraint.
   * @param specification the specification to wrap
   * @return a configured {@link Specification}
   * @param <T> the type of the {@link Specification} {@link Root}
   */
  public static <T> Specification<T> distinct(Specification<T> specification) {
    return (root, query, criteriaBuilder) -> {
      query.distinct(true);
      return specification.toPredicate(root, query, criteriaBuilder);
    };
  }


  protected static <T> Predicate objectEquals(CriteriaBuilder criteriaBuilder,
      Expression<T> expression, T other) {
    return criteriaBuilder.equal(expression, criteriaBuilder.literal(other));
  }
  protected static Predicate contains(CriteriaBuilder criteriaBuilder,
      Expression<String> property,
      String searchTerm) {
    return criteriaBuilder.like(criteriaBuilder.lower(property),
        "%" + searchTerm.strip().toLowerCase() + "%");
  }


  protected static Predicate jsonContains(CriteriaBuilder criteriaBuilder,
      Expression<?> jsonProperty,
      String jsonPath, String searchTerm) {
    return criteriaBuilder.isNotNull(
        searchJson(criteriaBuilder, jsonProperty, jsonPath,
            "%" + searchTerm.strip().toLowerCase() + "%")
    );
  }

  /**
   * Turns milliseconds into an offset String
   *
   * @param offsetMillis
   * @return
   */
  protected static String formatMillisToOffsetString(int offsetMillis) {
    var prefix = offsetMillis >= 0 ? "+" : "";
    var separator = ":";
    var seconds = offsetMillis / 1000;
    int minutes = Math.divideExact(seconds, 60);
    int hours = Math.divideExact(minutes, 60);
    minutes = minutes - hours * 60;
    DecimalFormat decimalFormat = new DecimalFormat("##00");
    return prefix + decimalFormat.format(hours) + separator + decimalFormat.format(minutes);
  }

  protected static Expression<LocalDateTime> toClientTime(CriteriaBuilder criteriaBuilder,
      Expression<Instant> instant, String clientTimeZone) {
    //https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/convert_tz
    return criteriaBuilder.function("CONVERT_TZ",
        LocalDateTime.class,
        instant,
        //always +00:00 as java.time.Instant is always UTC
        criteriaBuilder.literal("+00:00"),
        criteriaBuilder.literal(clientTimeZone));
  }

  protected static Expression<String> formatDate(CriteriaBuilder criteriaBuilder,
      Expression<LocalDateTime> dateTime, String pattern) {
    //https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/date_format
    return criteriaBuilder.function("DATE_FORMAT",
        String.class,
        dateTime,
        criteriaBuilder.literal(pattern));
  }

  protected static Expression<String> extractFormattedLocalDate(CriteriaBuilder criteriaBuilder,
      Expression<Instant> instant, int clientTimeOffsetMillis, String dateTimePattern) {
    return formatDate(criteriaBuilder,
        toClientTime(criteriaBuilder, instant,
            formatMillisToOffsetString(clientTimeOffsetMillis)),
        dateTimePattern);
  }

  /**
   *
   * @param criteriaBuilder
   * @param jsonDoc         an expression providing a valid JSON document.
   * @param jsonPath        <a
   *                        href="https://mariadb.com/docs/server/reference/sql-functions/special-functions/json-functions/jsonpath-expressions">JSONPath
   *                        Expression</a>
   * @param <T>
   * @return
   */
  protected static <T> Expression<T> extractFromJson(CriteriaBuilder criteriaBuilder,
      Expression<?> jsonDoc,
      String jsonPath, Class<T> expectedType) {
    //https://mariadb.com/docs/server/reference/sql-functions/special-functions/json-functions/json_extract
    return criteriaBuilder.function("JSON_EXTRACT", expectedType, jsonDoc,
        criteriaBuilder.literal(jsonPath));
  }

  protected static Expression<String> searchJson(CriteriaBuilder criteriaBuilder,
      Expression<?> jsonDoc,
      String jsonPath, String searchTerm) {
//    https://mariadb.com/docs/server/reference/sql-functions/special-functions/json-functions/json_search
    return criteriaBuilder.function("JSON_SEARCH", String.class,
        jsonDoc,
        criteriaBuilder.literal("one"),
        criteriaBuilder.literal(searchTerm),
        criteriaBuilder.nullLiteral(String.class),
        criteriaBuilder.literal(jsonPath));
  }

}
