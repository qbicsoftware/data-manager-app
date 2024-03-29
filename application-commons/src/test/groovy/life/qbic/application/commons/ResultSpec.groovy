package life.qbic.application.commons


import spock.lang.Specification

class ResultSpec extends Specification {
    def "construct from value"() {
        when:
        var result = Result.fromValue(5)
        Result<Integer, String> typedResult = Result.fromValue(5)
        then:
        result instanceof Result<Integer, ?>
        typedResult instanceof Result<Integer, String>
    }

    def "construct from error"() {
        when:
        var result = Result.fromError("Oh no!")
        Result<Integer, String> typedResult = Result.fromError("Oh no!")
        then:
        result instanceof Result<?, Integer>
        typedResult instanceof Result<Integer, String>
    }
}
