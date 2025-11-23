package life.qbic.datamanager.signposting.http;

import java.util.Optional;

/**
 * <record short description>
 *
 * @since <version tag>
 */
public record LinkParameter(String name, String value) {
  public static LinkParameter create(String name, String value) {
    return new LinkParameter(name, value);
  }

  public static LinkParameter createWithoutValue(String name) {
    return new LinkParameter(name, null);
  }

  public Optional<String> optionalValue() {
    return Optional.ofNullable(value);
  }
}
