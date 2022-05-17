package life.qbic.events

import life.qbic.usermanagement.registration.UserRegistered
import spock.lang.Specification

import java.time.Instant

class DomainEventSerializerSpec extends Specification {

  def "expect the serialized UserRegistered events contain the userId and occurrence time"() {
    given:
    DomainEventSerializer domainEventSerializer = new DomainEventSerializer()
    def userId = "test"
    def userRegistered = UserRegistered.create(userId)
    def eventOccurredOn = userRegistered.occurredOn().toString()

    expect:
    domainEventSerializer.serialize(userRegistered).contains("userId=" + userId)
    domainEventSerializer.serialize(userRegistered).contains("occurredOn=" + eventOccurredOn)
  }

  def "expect serialization of an unknown EventType does not work"() {
    given:
    DomainEventSerializer domainEventSerializer = new DomainEventSerializer()
    DomainEvent unknownType = new DomainEvent() {
      @Override
      Instant occurredOn() {
        Instant.now()
      }
    }
    when:
    domainEventSerializer.serialize(unknownType)
    then:
    thrown(UnrecognizedEventTypeException)
  }

}
