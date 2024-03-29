package life.qbic.identity.domain.user.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import life.qbic.identity.domain.event.UserRegistered
import spock.lang.Specification

class UserRegisteredEventSpec extends Specification {

    def "Ensure serialisation and deserialisation"() {
        given:
        UserRegistered userRegistered = UserRegistered.create("1234", "Sven", "sven.fillinger@test.de")

        when:
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build()
        String json = objectMapper.writeValueAsString(userRegistered)
        UserRegistered deserialised = objectMapper.readValue(json, UserRegistered)

        then:
        userRegistered == deserialised

    }

}
