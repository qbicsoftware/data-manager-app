package life.qbic.datamanager.signposting.http;

import java.util.Optional;

/**
 * <record short description>
 *
 * @since <version tag>
 */
public record WebLinkParameter(String name, String value) {
  public static WebLinkParameter create(String name, String value) {
    return new WebLinkParameter(name, value);
  }

  public static WebLinkParameter createWithoutValue(String name) {
    return new WebLinkParameter(name, null);
  }

  public Optional<String> optionalValue() {
    return Optional.ofNullable(value);
  }
}
