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
class ValueTest extends Specification {

    static Either<String, Integer> value = Either.fromValue("test")

    def "on value calls the consumer"() {
        given:
        var value = Either.fromValue("test")
        Consumer<String> consumer = Mock()
        when:
        def result = value.onValue(consumer)
        then:
        1 * consumer.accept(_)
        result.is(value)
    }

    def "on error returns this"() {
        given:
        Consumer<Integer> consumer = Mock()
        when:
        def result = value.onError(consumer)
        then:
        0 * consumer.accept(_)
        result.is(value)
    }

    def "is value returns true"() {
        expect:
        value.isValue()
    }

    def "is error returns false"() {
        expect:
        !value.isError()
    }

    def "transform value returns an either with transformed value"() {

    }

    def "transform error returns an either with unchanged error"() {

    }
}
