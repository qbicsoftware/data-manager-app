package life.qbic.application.commons.tmp

import spock.lang.Specification

import java.util.function.Consumer

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class ErrorTest extends Specification {
    static Either<Integer, String> error = Either.fromError("Oh no, an error!")

    def "on value returns this"() {
        given:
        Consumer<Integer> consumer = Mock()
        when:
        def result = error.onValue(consumer)
        then:
        0 * consumer.accept(_)
        result.is(error)
    }

    def "on error calls the consumer"() {
        given:
        Consumer<String> consumer = Mock()
        when:
        def result = error.onError(consumer)
        then:
        1 * consumer.accept(_)
        result.is(error)
    }

    def "is value returns false"() {
        expect:
        !error.isValue()
    }

    def "is error returns true"() {
        expect:
        error.isError()
    }

    def "transform value returns an either with unchanged value"() {

    }

    def "transform error returns an either with transformed error"() {

    }
}
