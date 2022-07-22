package life.qbic.identity.domain.user.event;

import java.util.Set;
import life.qbic.identity.domain.event.DomainEvent;

public interface EventStore {

  void append(DomainEvent event);

  public Set<DomainEvent> findAllByType(Class<DomainEvent> type);
}
