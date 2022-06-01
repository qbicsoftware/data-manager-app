package life.qbic.events;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.domain.events.DomainEvent;

/**
 * An in-memory class acting as an event repository.
 *
 * @since 1.0.0
 */
public class TemporaryEventRepository implements EventRepository {

  private final Set<StoredEvent> storedEvents = new HashSet<>();
  private long latestEventId = 0;

  @Override
  public List<StoredEvent> findAllByType(Class<DomainEvent> type) {
    return storedEvents.stream()
        .filter(it -> it.typeName().equals(type.getName()))
        .collect(Collectors.toList());
  }

  @Override
  public void save(StoredEvent storedEvent) {
    long eventId = latestEventId + 1;
    storedEvent.setEventId(eventId);
    storedEvents.add(storedEvent);
    latestEventId = eventId;
  }
}
