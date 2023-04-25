package life.qbic.domain.concepts;

import java.time.Instant;

public class StoredEvent {
  private final String eventBody;
  private long eventId;
  private final Instant occurredOn;
  private final String typeName;

  public StoredEvent(String eventBody, Instant occurredOn, String typeName) {
    this.eventBody = eventBody;
    this.occurredOn = occurredOn;
    this.typeName = typeName;
  }

  public String eventBody() {
    return eventBody;
  }

  public long eventId() {
    return eventId;
  }

  public Instant occurredOn() {
    return occurredOn;
  }

  public String typeName() {
    return typeName;
  }

  public void setEventId(long eventId) {
    this.eventId = eventId;
  }

  @Override
  public String toString() {
    return "StoredEvent{"
        + "eventBody='"
        + eventBody
        + '\''
        + ", eventId="
        + eventId
        + ", occurredOn="
        + occurredOn
        + ", typeName='"
        + typeName
        + '\''
        + '}';
  }
}
