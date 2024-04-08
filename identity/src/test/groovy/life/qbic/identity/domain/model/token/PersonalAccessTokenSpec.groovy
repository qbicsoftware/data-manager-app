package life.qbic.identity.domain.model.token

import spock.lang.Specification

import java.time.Duration

class PersonalAccessTokenSpec extends Specification {

    def "When the expiry date has been reached, a token must not be valid anymore"() {
        given:
        PersonalAccessToken token = new PersonalAccessToken("a-user-id", "a description", Duration.ofMillis(20), "test")

        when:
        Thread.sleep(100)

        then:
        token.hasExpired()
    }

    def "When the expiry date has not been reached yet, a token must still be valid"() {
        given:
        PersonalAccessToken token = new PersonalAccessToken("a-user-id", "a description", Duration.ofDays(20), "test")

        when:
        Thread.sleep(100)

        then:
        !token.hasExpired()
    }

}
