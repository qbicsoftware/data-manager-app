package life.qbic.logging.subscription.provider.mail.property;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import java.util.Properties;

/**
 * <b>Property File Parser</b>
 * <p>
 * Parses a property file and saves all properties in a {@link Properties} object. The property
 * syntax must follow the convention:
 * <p>
 * <code>property-name=property-value</code>
 * <p>
 * separated by line.
 * <p>
 * The parser also supports automated resolvent of placeholders, with a default behaviour that
 * resolves them against available system environment variables.
 * <p>
 * The syntax for placeholders is like the following:
 * <p>
 * <code>property-name=${MY_ENV_VAR}</code>
 * <p>
 * The resolvent of the placeholder is case sensitive, so make sure that it has the same
 * capitalization as the environment variable.
 *
 * @since 1.0.0
 */
public class PropertyFileParser {

  private PropertyFileParser() {}

  /**
   * Parses a file for defined properties and resolves present placeholder against visible
   * environment variables.
   *
   * @param file properties file containing line-separated property tuples
   * @return a {@link Properties} object with the parsed properties
   * @throws IOException if the file cannot be accessed
   * @since 1.0.0
   */
  public static Properties parse(File file) throws IOException {
    requireNonNull(file, "File must not be null");

    var properties = new Properties();

    try (var fileInputStream = new FileInputStream(file)) {
      properties.load(fileInputStream);
    }

    properties = resolvePlaceholders(properties);

    return properties;
  }

  /**
   * Parses an input stream for defined properties and resolves present placeholder against visible
   * environment variables.
   *
   * @param inputStream the input stream with the defined properties
   * @return a {@link Properties} object with the parsed properties
   * @throws IOException if the file cannot be accessed
   * @since 1.0.0
   */
  public static Properties parse(InputStream inputStream) throws IOException {
    requireNonNull(inputStream, "Input stream must not be null");

    var properties = new Properties();
    properties.load(inputStream);

    properties = resolvePlaceholders(properties);

    return properties;
  }

  private static Properties resolvePlaceholders(Properties properties) {
    var resolvedProperties = new Properties();
    properties.forEach((property, value) -> {
      try {
        var placeholder = Placeholder.create((String) value);
        var envVarValue = EnvironmentVariableResolver.resolve(
            placeholder.name());
        if (isNull(envVarValue)) {
          throw new IllegalArgumentException("Could not resolve placeholder '" + value + "'");
        }
        resolvedProperties.setProperty((String) property, envVarValue);
      } catch (IllegalArgumentException ignored) {
        resolvedProperties.setProperty((String) property, (String) value);
      }
    });
    return resolvedProperties;
  }

}
