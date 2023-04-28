package life.qbic.domain.concepts;

import java.io.*;
import java.util.Base64;

public class DomainEventSerializer {

  public <T extends DomainEvent> String serialize(T event) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(event);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public DomainEvent deserialize(String serializedEvent) {
    try (ByteArrayInputStream bais =
             new ByteArrayInputStream(Base64.getDecoder().decode(serializedEvent));
         ObjectInputStream ois = new ObjectInputStream(bais)) {
      return (DomainEvent) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
