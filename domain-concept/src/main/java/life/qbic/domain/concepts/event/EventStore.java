package life.qbic.domain.concepts.event;

import life.qbic.domain.concepts.DomainEvent;

import java.util.Set;

public interface EventStore {

  void append(DomainEvent event);

  Set<DomainEvent> findAllByType(Class<DomainEvent> type);
}
