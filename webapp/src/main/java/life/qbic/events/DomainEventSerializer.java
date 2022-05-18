package life.qbic.events;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import life.qbic.usermanagement.registration.UserRegistered;

public class DomainEventSerializer {

  public <T extends DomainEvent & Serializable> String serialize(T event) {
    try (
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)
    ) {
      oos.writeObject(event);
      oos.close();
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public DomainEvent deserialize(String serializedEvent, String desiredType) {
    try (
        ByteArrayInputStream bais = new ByteArrayInputStream(
            serializedEvent.getBytes(StandardCharsets.UTF_8));
        ObjectInputStream ois = new ObjectInputStream(bais)
    ) {
      return (DomainEvent) Class.forName(desiredType).cast(ois.readObject());
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static class UserRegisteredSerializer {

    static String serialize(UserRegistered event) {
      HashMap<String, String> propertiesMap = new HashMap<>();
      propertiesMap.put("userId", event.getUserId());
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
        throw new RuntimeException(
            "Cannot deserialize to type " + UserRegistered.class.getName());
      } else if (occurredOn == null) {
        throw new RuntimeException(
            "Cannot deserialize to type " + UserRegistered.class.getName());
      }
      return UserRegistered.create(userId, occurredOn);
    }
  }

}
