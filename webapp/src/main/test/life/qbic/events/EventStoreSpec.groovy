package life.qbic.events

import life.qbic.domain.usermanagement.registration.UserRegistered
import spock.lang.Specification

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class EventStoreSpec extends Specification {
  UserRegistered userRegisteredEvent = UserRegistered.create("my.awesome@user.id", "", "")

  def "when a domain event is appended to the event store, then no exception is thrown"() {
    when: "a domain event is appended to the event store"
    EventStore.instance(new TemporaryEventRepository()).append(userRegisteredEvent)
    then: "no exception is thrown"
    noExceptionThrown()
  }

  def "after a domain event is appended to the event store, it can be retrieved by type "() {
    given:
    def eventStore = EventStore.instance(new TemporaryEventRepository())

    when:
    eventStore.append(userRegisteredEvent)

    then:
    eventStore.findAllByType(UserRegistered).contains(userRegisteredEvent)
  }

  def "when the same event is appended multiple times, then it is found only once"() {
    given:
    EventStore eventStore = EventStore.instance(new TemporaryEventRepository())

    when: "the same event is appended multiple times"
    eventStore.append(userRegisteredEvent)
    eventStore.append(userRegisteredEvent)

    then: "it is found only once"
    1 == eventStore.findAllByType(UserRegistered).count { it == userRegisteredEvent }
  }

}
