package life.qbic.events;

import java.util.Set;

/**
 * Implementation of a basic event store. It handles events and provides accessor methods to retain
 * the events later.
 */
public class EventStore {

  private final EventRepository eventRepository;

  public EventStore(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
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
    return eventRepository.findAllByType(type);
  }

}
