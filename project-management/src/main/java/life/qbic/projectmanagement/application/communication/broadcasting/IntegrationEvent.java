package life.qbic.projectmanagement.application.communication.broadcasting;

import java.util.Map;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record IntegrationEvent(String type, Map<String, String> content) {

  public static IntegrationEvent create(String type, Map<String, String> content) {
    return new IntegrationEvent(type, content);
  }

}
