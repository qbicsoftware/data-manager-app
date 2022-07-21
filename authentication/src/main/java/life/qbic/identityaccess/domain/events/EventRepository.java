package life.qbic.identityaccess.domain.events;

import java.util.List;
import life.qbic.identityaccess.domain.events.DomainEvent;

/**
 * A repository for domain events.
 *
 * @since 1.0.0
 */
public interface EventRepository {

  List<StoredEvent> findAllByType(Class<DomainEvent> type);

  void save(StoredEvent storedEvent);
}
