package life.qbic.authentication.domain.event

import life.qbic.authentication.domain.user.event.UserRegistered
import spock.lang.Specification

class DomainEventSerializerSpec extends Specification {
    final DomainEventSerializer domainEventSerializer = new DomainEventSerializer()
    final def userRegistered = UserRegistered.create("test", "test", "test")

    def "expect the serialized UserRegistered events are non empty Strings"() {
        expect:
        !domainEventSerializer.serialize(userRegistered).isEmpty()
    }

    def "expect the deserialized DomainEvents are the same as before serialization"() {
        expect:
        userRegistered == domainEventSerializer.deserialize(domainEventSerializer.serialize(userRegistered))

    }


}
