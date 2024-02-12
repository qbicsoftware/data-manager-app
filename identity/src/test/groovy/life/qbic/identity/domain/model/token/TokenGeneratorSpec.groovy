package life.qbic.identity.domain.model.token


import spock.lang.Specification

class TokenGeneratorSpec extends Specification {

    def "Generated token must be different from the previous ones"() {
        given:
        TokenGenerator tokenGenerator = new TokenGenerator()

        when:
        def firstToken = tokenGenerator.token()
        def secondToken = tokenGenerator.token()

        then:
        firstToken != secondToken
    }

}
