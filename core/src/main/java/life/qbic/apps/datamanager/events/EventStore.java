package life.qbic.apps.datamanager.events;

import java.util.Set;
import life.qbic.identityaccess.domain.events.DomainEvent;

public interface EventStore {

  void append(DomainEvent event);

  public Set<DomainEvent> findAllByType(Class<DomainEvent> type);
}
