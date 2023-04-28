package life.qbic.domain.concepts


import spock.lang.Specification

class SimpleEventStoreSpec extends Specification {
    TestEvent testEvent = new TestEvent()

    def "when a domain event is appended to the event store, then no exception is thrown"() {
        when: "a domain event is appended to the event store"

        def eventStore = SimpleEventStore.instance(new TemporaryEventRepository())
        eventStore.append(testEvent)
        then: "no exception is thrown"
        noExceptionThrown()
    }

    def "after a domain event is appended to the event store, it can be retrieved by type "() {
        given:
        def eventStore = SimpleEventStore.instance(new TemporaryEventRepository())

        when:
        eventStore.append(testEvent)

        then:
        eventStore.findAllByType(TestEvent).contains(testEvent)
    }

    def "when the same event is appended multiple times, then it is found only once"() {
        given:
        SimpleEventStore eventStore = SimpleEventStore.instance(new TemporaryEventRepository())

        when: "the same event is appended multiple times"
        eventStore.append(testEvent)
        eventStore.append(testEvent)

        then: "it is found only once"
        1 == eventStore.findAllByType(TestEvent).count { it == testEvent }
    }

}
