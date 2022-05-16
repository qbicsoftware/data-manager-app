package life.qbic.events;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class TemporaryEventRepository implements EventRepository {

  private final Set<StoredEvent> storedEvents = new HashSet<>();
  private long latestEventId = 0;

  @Override
  public void save(StoredEvent storedEvent) {
    long eventId = latestEventId + 1;
    storedEvent.setEventId(eventId);
    storedEvents.add(storedEvent);
    latestEventId = eventId;
  }

  @Override
  public Set<DomainEvent> findAllByType(Class<DomainEvent> type) {
    return storedEvents.stream()
        .filter(it -> it.typeName().equals(type.getName()))
        .map(it -> EventStore.eventSerializer().deserialize(it.eventBody(), type.getName()))
        .collect(Collectors.toSet());
  }
}
