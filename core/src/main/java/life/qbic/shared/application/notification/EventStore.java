package life.qbic.shared.application.notification;

import java.util.Set;
import life.qbic.shared.domain.events.DomainEvent;

public interface EventStore {

  void append(DomainEvent event);

  public Set<DomainEvent> findAllByType(Class<DomainEvent> type);
}
