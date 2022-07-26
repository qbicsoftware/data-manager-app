package life.qbic.authentication.domain.event


import life.qbic.authentication.domain.user.event.UserRegistered
import spock.lang.Specification

class EventStoreSpec extends Specification {
    UserRegistered userRegisteredEvent = UserRegistered.create("my.awesome@user.id", "", "")

    def "when a domain event is appended to the event store, then no exception is thrown"() {
        when: "a domain event is appended to the event store"
        SimpleEventStore.instance(new TemporaryEventRepository()).append(userRegisteredEvent)
        then: "no exception is thrown"
        noExceptionThrown()
    }

    def "after a domain event is appended to the event store, it can be retrieved by type "() {
        given:
        def eventStore = SimpleEventStore.instance(new TemporaryEventRepository())

        when:
        eventStore.append(userRegisteredEvent)

        then:
        eventStore.findAllByType(UserRegistered).contains(userRegisteredEvent)
    }

    def "when the same event is appended multiple times, then it is found only once"() {
        given:
        SimpleEventStore eventStore = SimpleEventStore.instance(new TemporaryEventRepository())

        when: "the same event is appended multiple times"
        eventStore.append(userRegisteredEvent)
        eventStore.append(userRegisteredEvent)

        then: "it is found only once"
        1 == eventStore.findAllByType(UserRegistered).count { it == userRegisteredEvent }
    }

}
