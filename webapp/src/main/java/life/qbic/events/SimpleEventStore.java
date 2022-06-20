package life.qbic.events;

import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.shared.application.notification.EventStore;
import life.qbic.shared.domain.events.DomainEvent;

/**
 * Implementation of a basic event store. It handles events and provides accessor methods to retain
 * the events later.
 */
public class SimpleEventStore implements EventStore {

  private static SimpleEventStore instance;

  private final EventRepository eventRepository;

  private SimpleEventStore(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public static SimpleEventStore instance(EventRepository eventRepository) {
    if (instance == null || !instance.eventRepository.equals(eventRepository)) {
      instance = new SimpleEventStore(eventRepository);
    }
    return instance;
  }

  @Override
  public void append(DomainEvent event) {
    String eventSerialization = SimpleEventStore.eventSerializer().serialize(event);
    StoredEvent storedEvent =
        new StoredEvent(eventSerialization, event.occurredOn(), event.getClass().getName());
    eventRepository.save(storedEvent);
  }

  @Override
  public Set<DomainEvent> findAllByType(Class<DomainEvent> type) {
    var storedEvents = eventRepository.findAllByType(type);
    return storedEvents.stream()
        .map(it -> SimpleEventStore.eventSerializer().deserialize(it.eventBody()))
        .collect(Collectors.toSet());
  }

  private static DomainEventSerializer eventSerializer() {
    return new DomainEventSerializer();
  }
}
