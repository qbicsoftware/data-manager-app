package life.qbic.events;

import java.util.Set;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface EventRepository {

  Set<DomainEvent> findAllByType(Class<DomainEvent> type);
  void save(StoredEvent storedEvent);

}
