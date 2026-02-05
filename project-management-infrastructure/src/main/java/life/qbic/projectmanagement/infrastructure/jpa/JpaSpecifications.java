package life.qbic.projectmanagement.infrastructure.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;


/**
 * Utility class providing reusable helper methods for constructing Spring Data JPA
 * {@link org.springframework.data.jpa.domain.Specification} instances.
 *
 * <h2>Intention</h2>
 * <p>
 * This class centralizes common patterns for building query predicates based on entity
 * properties, property names, or expressions. It is designed as a collection of small,
 * reusable building blocks that simplify working with the JPA Criteria API and reduce
 * boilerplate code across the persistence layer.
 * </p>
 *
 * <h2>Relation to JPA Specifications</h2>
 * <p>
 * Spring Data JPA {@link org.springframework.data.jpa.domain.Specification} follows the
 * Composite pattern, allowing complex queries to be built by combining smaller predicates
 * using {@code and(...)} and {@code or(...)}. The methods in this class act as reusable
 * "leaf" specifications that can be composed into more complex query structures.
 * </p>
 *
 * <p>
 * This class is intentionally stateless and focuses solely on supporting the creation of
 * composable {@code Specification} instances that can be assembled where needed.
 * </p>
 */

public class JpaSpecifications {

  private static final Logger log = LoggerFactory.logger(JpaSpecifications.class);
  /**
   * A custom date time format not in any java.time ISO formats
   */
  public static final String CUSTOM_DATE_TIME_PATTERN = "%Y-%m-%d %H:%i";


  /**
   * Interface for resolving a JPA {@link jakarta.persistence.criteria.Expression}
   * from a given source expression.
   *
   * <h2>Intention</h2>
   * <p>
   * An {@code ExpressionProvider} describes how to navigate from an existing JPA Criteria
   * expression (for example a {@code Root}, {@code From}, or another {@code Expression})
   * to a target expression representing a specific attribute or derived value.
   * </p>
   *
   * <p>
   * It does not extract runtime values from entities. Instead, it defines how to build
   * a new {@link jakarta.persistence.criteria.Expression} that can later be used to
   * construct predicates in a {@link org.springframework.data.jpa.domain.Specification}.
   * </p>
   *
   * <h2>Usage context</h2>
   * <p>
   * This abstraction is especially useful when building reusable specification helpers.
   * Instead of referring to entity attributes via string-based paths, callers can provide
   * an {@code ExpressionProvider} that encapsulates how to access or derive the relevant
   * expression. This improves readability, reuse, and type-safety when constructing
   * dynamic queries with the JPA Criteria API.
   * </p>
   *
   * @param <S> the type represented by the source type (e.g. MyEntity)
   * @param <T> the type represented by the resolved expression (e.g. String)
   * @param <X> the source type from which the expression is resolved (e.g. Root<MyEntity>)
   */
  @FunctionalInterface
  public interface ExpressionProvider<S, T, X extends Expression<S>> extends
      Function<X, Expression<T>> {

    @Override
    default Expression<T> apply(X expression) {
      return extractFrom(expression);
    }

    /**
     * Resolves a target {@link jakarta.persistence.criteria.Expression} from the given source
     * expression.
     *
     * <p>
     * The source is typically a {@code Root<T>}, {@code From<?, ?>}, or another expression
     * that serves as the starting point for navigating to an attribute. Implementations
     * define how to derive the resulting expression, for example by:
     * </p>
     * <ul>
     *   <li>Accessing an entity attribute</li>
     *   <li>Navigating a join path</li>
     *   <li>Returning a nested property expression</li>
     *   <li>Applying a transformation or function</li>
     * </ul>
     *
     * <p>
     * The returned expression can then be used to build predicates such as equality,
     * LIKE, range, or null checks within a {@link org.springframework.data.jpa.domain.Specification}.
     * </p>
     *
     * @param expression the source expression from which the target expression should be derived
     * @return the resolved expression representing the desired attribute or value
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
   * @param expressionProvider an {@link ExpressionProvider} that provides an {@link Expression} of type {@link String}
   * @param searchTerm the term to search for
   * @return a configured Specification
   * @param <T> the type of the {@link Specification} {@link Root}
   */
  public static <T> Specification<T> contains(
      ExpressionProvider<T, String, Root<T>> expressionProvider,
      String searchTerm) {
    return (root, query, criteriaBuilder) ->
        contains(criteriaBuilder, expressionProvider.extractFrom(root), searchTerm);
  }

  /**
   * Creates a {@link Specification} that passes when value of an {@link Expression} equals the provided value.
   * @param expressionProvider an {@link ExpressionProvider} that provides an {@link Expression} of type {@link S}
   * @param other the object to compare to
   * @return a configured Specification
   * @param <S> the type of item that is compared for equality
   * @param <T> the type of the {@link Specification} {@link Root}
   */
  public static <S, T> Specification<T> exactMatches(
      ExpressionProvider<T, S, Root<T>> expressionProvider,
      S other
  ) {
    return (root, query, criteriaBuilder) ->
        objectEquals(criteriaBuilder, expressionProvider.extractFrom(root), other);
  }

  /**
   * Creates a {@link Specification} that passes when the formatted value of an {@link Expression} contains the search term.
   * @param instantPropertyName the name of the property of type {@link Instant}
   * @param searchTerm the term to search for
   * @param clientOffsetMillis the millisecond offset used to deduct the client time zone
   * @param dateTimePattern the format pattern to format the time as specified by <a href="https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/date_format">DATE_FORMAT</a>. The formatted time is searched for containing the search term.
   * @return a configured Specification
   * @param <T> the type of the {@link Specification} {@link Root}
   * @see <a href="https://mariadb.com/docs/server/reference/sql-functions/date-time-functions/date_format">DATE_FORMAT</a>
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
   * @param expressionProvider an {@link ExpressionProvider} that provides an {@link Expression} of type {@link S}
   * @param jsonPath json path to be searched
   * @param searchTerm the term to search for
   * @return a configured Specification
   * @param <S> the type of item that is compared for equality
   * @param <T> the type of the {@link Specification} {@link Root}
   * @see <a href="https://mariadb.com/docs/server/reference/sql-functions/special-functions/json-functions/jsonpath-expressions">JSONPath Expressions</a>
   */
  public static <S, T> Specification<S> jsonContains(
      ExpressionProvider<S, T, Root<S>> expressionProvider,
      String jsonPath, String searchTerm) {
    return (root, query, criteriaBuilder) ->
        jsonContains(criteriaBuilder, expressionProvider.extractFrom(root), jsonPath, searchTerm);

  }

  /**
   * Wraps a {@link Specification} in a distinct constraint.
   * @param specification the specification to wrap
   * @return a configured {@link Specification}
   * @param <T> the type of the {@link Specification} {@link Root}
   */
  public static <T> Specification<T> distinct(Specification<T> specification) {
    return (root, query, criteriaBuilder) -> {
      if (Objects.isNull(query)) {
        return criteriaBuilder.disjunction();
      }
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
