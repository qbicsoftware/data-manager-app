package life.qbic.domain.concepts.event


import life.qbic.domain.concepts.DomainEvent
import life.qbic.domain.concepts.DomainEventDispatcher
import life.qbic.domain.concepts.DomainEventSubscriber
import spock.lang.Specification

import java.time.Instant

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class DomainEventDispatcherSpec extends Specification {

    def "Given a number of N domain event subscriber for an event E, the dispatcher needs to call all N subscriber if an event E is dispatched"() {
        given:
        DomainEventSubscriber<TestDomainEvent> subscriberOne = Mock(DomainEventSubscriber<TestDomainEvent>)
        DomainEventSubscriber<TestDomainEvent> subscriberTwo = Mock(DomainEventSubscriber<TestDomainEvent>)
        subscriberOne.subscribedToEventType() >> TestDomainEvent.class
        subscriberTwo.subscribedToEventType() >> TestDomainEvent.class

        and:
        DomainEventDispatcher dispatcher = DomainEventDispatcher.instance()

        and:
        dispatcher.subscribe(subscriberOne)
        dispatcher.subscribe(subscriberTwo)

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

}
