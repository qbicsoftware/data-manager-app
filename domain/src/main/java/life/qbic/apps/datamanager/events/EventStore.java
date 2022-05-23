package life.qbic.apps.datamanager.events;

import life.qbic.domain.events.DomainEvent;

import java.util.Set;

public interface EventStore {

    void append(DomainEvent event);

    public Set<DomainEvent> findAllByType(Class<DomainEvent> type);

}
