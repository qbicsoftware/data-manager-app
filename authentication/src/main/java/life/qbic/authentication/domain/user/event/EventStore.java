package life.qbic.authentication.domain.user.event;

import java.util.Set;
import life.qbic.authentication.domain.event.DomainEvent;

public interface EventStore {

  void append(DomainEvent event);

  public Set<DomainEvent> findAllByType(Class<DomainEvent> type);
}
