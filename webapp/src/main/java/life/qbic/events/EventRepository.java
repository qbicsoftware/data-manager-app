package life.qbic.events;

import java.util.List;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface EventRepository {

  List<StoredEvent> findAllByType(Class<DomainEvent> type);
  void save(StoredEvent storedEvent);

}
