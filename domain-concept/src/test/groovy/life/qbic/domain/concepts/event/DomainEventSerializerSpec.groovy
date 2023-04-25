package life.qbic.domain.concepts.event


import life.qbic.domain.concepts.DomainEventSerializer
import spock.lang.Specification

class DomainEventSerializerSpec extends Specification {
    final DomainEventSerializer domainEventSerializer = new DomainEventSerializer()
    final def userRegistered = new TestEvent()

    def "expect the serialized UserRegistered events are non empty Strings"() {
        expect:
        !domainEventSerializer.serialize(userRegistered).isEmpty()
    }

    def "expect the deserialized DomainEvents are the same as before serialization"() {
        expect:
        userRegistered == domainEventSerializer.deserialize(domainEventSerializer.serialize(userRegistered))

    }

}

