package life.qbic.projectmanagement.application.communication.broadcasting;

import java.util.Map;

/**
 * <b>Integration Event</b>
 * <p>
 * Integration events can be used to broadcast events from one domain to multiple other domains.
 * <p>
 * Integration event have types. Types are describing concisely what the event is about. For example
 * if a user has registered, the type can be "userRegistered". Types can be used to provide some
 * semantics any potential receiver might be interested in, or for filtering.
 * <p>
 * Integration events should be simple and not expose domain logic. Therefore, the current
 * implementation only uses a simple key:value pair pattern for the content that is giving some
 * additional information for the event type.
 * <p>
 * In the above example of an user registered event, one can provide the user's id with the event,
 * such that any downstream domain can do additional user information queries based on the id.
 *
 * @since 1.0.0
 */
public record IntegrationEvent(String type, Map<String, String> content) {

  /**
   * @param type    a semantically concise term for the event type
   * @param content additional information about the event in the key-value pair pattern
   * @return an integration event
   * @since 1.0.0
   */
  public static IntegrationEvent create(String type, Map<String, String> content) {
    return new IntegrationEvent(type, content);
  }
}

