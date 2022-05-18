package life.qbic.events;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of a basic event store. It handles events and provides accessor methods to retain
 * the events later.
 */
public class EventStore {

  private static EventStore instance;

  private final EventRepository eventRepository;

  public EventStore(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public static EventStore getInstance(EventRepository eventRepository) {
    if (instance == null || !instance.eventRepository.equals(eventRepository)) {
      instance = new EventStore(eventRepository);
    }
    return instance;
  }

  public void append(DomainEvent event) {
    String eventSerialization = EventStore.eventSerializer().serialize(event);
    StoredEvent storedEvent = new StoredEvent(
        eventSerialization,
        event.occurredOn(),
        event.getClass().getName());
    eventRepository.save(storedEvent);
  }

  static DomainEventSerializer eventSerializer() {
    return new DomainEventSerializer();
  }

  public Set<DomainEvent> findAllByType(Class<DomainEvent> type) {
    var storedEvents = eventRepository.findAllByType(type);
    return storedEvents.stream()
        .map(it -> EventStore.eventSerializer().deserialize(it.eventBody(), type.getName()))
        .collect(Collectors.toSet());
  }
}
