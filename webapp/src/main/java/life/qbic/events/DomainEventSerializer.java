package life.qbic.events;


import java.time.Instant;
import java.util.HashMap;
import life.qbic.domain.events.DomainEvent;
import life.qbic.domain.usermanagement.registration.UserRegistered;

public class DomainEventSerializer {

  public <T extends DomainEvent> String serialize(T event) {
    if (event instanceof UserRegistered) {
      return UserRegisteredSerializer.serialize((UserRegistered) event);
    }
    throw
        new UnrecognizedEventTypeException(
        "Cannot serialize events of type " + event.getClass().getName() + ". Unknown event type.");
  }

  public DomainEvent deserialize(String serializedEvent, String desiredType) {
    if (desiredType.equals(UserRegistered.class.getName())) {
      return UserRegisteredSerializer.deserialize(serializedEvent);
    }
    throw new UnrecognizedEventTypeException(
        "Cannot deserialize events of type " + desiredType + ". Unknown event type.");
  }

  private static class UserRegisteredSerializer {

    static String serialize(UserRegistered event) {
      HashMap<String, String> propertiesMap = new HashMap<>();
      propertiesMap.put("userId", event.userId());
      propertiesMap.put("occurredOn", event.occurredOn().toString());
      return propertiesMap.toString();
    }

    static UserRegistered deserialize(String serializedEvent) {
      var cleaned = serializedEvent.replaceAll("\\{", "").replaceAll("}", "");
      String[] propertyPairs = cleaned.split(", ");
      String userId = null;
      Instant occurredOn = null;
      for (String propertyPair : propertyPairs) {
        String[] keyValuePair = propertyPair.split("=");
        String key = keyValuePair[0];
        String value = keyValuePair[1];
        if (key.equals("userId")) {
          userId = value;
        } else if (key.equals("occurredOn")) {
          occurredOn = Instant.parse(value);
        }
      }
      if (userId == null) {
        throw new RuntimeException("Cannot deserialize to type " + UserRegistered.class.getName());
      } else if (occurredOn == null) {
        throw new RuntimeException("Cannot deserialize to type " + UserRegistered.class.getName());
      }
      return UserRegistered.createEvent(userId, "", "");
    }
  }

}
