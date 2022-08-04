package life.qbic.logging.subscription.provider.property;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class PropertyFileParser {

  public static Properties parse(File file) throws IllegalArgumentException, IOException {
    requireNonNull(file, "File must not be null");

    var properties = new Properties();
    properties.load(new FileInputStream(file));

    properties = resolvePlaceholders(properties);

    return properties;
  }

  private static Properties resolvePlaceholders(Properties properties) {
    var resolvedProperties = new Properties();
    properties.forEach( (property, value) -> {
      if (Placeholder.isPlaceholder((String) value)) {
        var envVarValue = EnvironmentVariableResolver.resolve(Placeholder.placeholderName((String) value));
        if (isNull(envVarValue)) {
          throw new IllegalArgumentException("Could not resolve placeholder '" + value + "'");
        }
        resolvedProperties.setProperty((String) property, envVarValue);
      } else {
        resolvedProperties.setProperty((String) property, (String) value);
      }
    });
    return resolvedProperties;
  }

}
