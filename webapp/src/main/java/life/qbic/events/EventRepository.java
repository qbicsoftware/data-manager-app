package life.qbic.events;

import java.util.List;
import life.qbic.shared.domain.events.DomainEvent;

/**
 * A repository for domain events.
 *
 * @since 1.0.0
 */
public interface EventRepository {

  List<StoredEvent> findAllByType(Class<DomainEvent> type);

  void save(StoredEvent storedEvent);
}
