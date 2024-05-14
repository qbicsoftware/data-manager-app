package life.qbic.domain.concepts.event


import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import spock.lang.Specification

import java.time.Instant

/**
 * Tests for the domain event dispatcher
 *
 * @since 1.0.0
 */
class DomainEventDispatcherSpec extends Specification {

    def "Given a number of N unique domain event subscribers for an event E, the dispatcher needs to call all N subscribers if an event E is dispatched"() {
        given:
        DomainEventSubscriber<TestDomainEvent> subscriberOne = Mock(DomainEventSubscriber<TestDomainEvent>)
        TestDomainEventSubscriber<TestDomainEvent> subscriberTwo = Mock(TestDomainEventSubscriber<TestDomainEvent>)
        subscriberOne.subscribedToEventType() >> TestDomainEvent.class
        subscriberTwo.subscribedToEventType() >> TestDomainEvent.class

        and:
        DomainEventDispatcher dispatcher = DomainEventDispatcher.instance()

        and:
        dispatcher.subscribe(subscriberOne)
        dispatcher.subscribe(subscriberTwo as DomainEventSubscriber<DomainEvent>)

        when:
        dispatcher.dispatch(new TestDomainEvent())

        then:
        1 * subscriberOne.handleEvent(_)
        1 * subscriberTwo.handleEvent(_)

    }

    class TestDomainEvent extends DomainEvent {

        @Serial
        static final long serialVersionUID = 12L

        @Override
        Instant occurredOn() {
            return null
        }
    }

    public interface TestDomainEventSubscriber<T extends DomainEvent> {

        /**
         * Query the subscribed domain event type.
         *
         * @return the domain event type that is subscribed to
         * @since 1.0.0
         */
        Class<? extends DomainEvent> subscribedToEventType();

        /**
         * Callback that will be executed by the publisher.
         *
         * <p>Passes the domain event of the type that was subscribed to.
         *
         * @param event the domain event
         * @since 1.0.0
         */
        void handleEvent(T event);
    }

}
