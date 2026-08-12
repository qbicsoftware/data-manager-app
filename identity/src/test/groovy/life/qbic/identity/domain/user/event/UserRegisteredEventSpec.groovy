package life.qbic.identity.domain.user.event

import life.qbic.identity.domain.event.UserRegistered
import spock.lang.Specification
import tools.jackson.databind.ObjectMapper

class UserRegisteredEventSpec extends Specification {

    def "Ensure serialisation and deserialisation"() {
        given:
        UserRegistered userRegistered = UserRegistered.create("1234", "Sven", "sven.fillinger@test.de")
        ObjectMapper objectMapper = new ObjectMapper();

        when:
        String json = objectMapper.writeValueAsString(userRegistered)
        UserRegistered deserialised = objectMapper.readValue(json, UserRegistered)

        then:
        userRegistered == deserialised

    }

}
