package life.qbic.authentication.domain.user.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
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
