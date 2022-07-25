package life.qbic.authentication.domain.event;

import java.util.List;

/**
 * A repository for domain events.
 *
 * @since 1.0.0
 */
public interface EventRepository {

  List<StoredEvent> findAllByType(Class<DomainEvent> type);

  void save(StoredEvent storedEvent);
}
