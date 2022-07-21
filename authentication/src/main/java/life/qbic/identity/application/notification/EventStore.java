package life.qbic.identity.application.notification;

import java.util.Set;
import life.qbic.identity.domain.events.DomainEvent;

public interface EventStore {

  void append(DomainEvent event);

  public Set<DomainEvent> findAllByType(Class<DomainEvent> type);
}
