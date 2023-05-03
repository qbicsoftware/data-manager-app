package life.qbic.domain.concepts.event;

import java.util.Set;
import life.qbic.domain.concepts.DomainEvent;

public interface EventStore {

  void append(DomainEvent event);

  Set<DomainEvent> findAllByType(Class<DomainEvent> type);
}
