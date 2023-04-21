package life.qbic.application.commons.tmp

import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class ValueSpec extends Specification {

    static Either<String, Integer> valueObject = Either.fromValue("test")

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
        def result = valueObject.onError(consumer)
        then:
        0 * consumer.accept(_)
        result.is(valueObject)
    }

    def "is value returns true"() {
        expect:
        valueObject.isValue()
    }

    def "is error returns false"() {
        expect:
        !valueObject.isError()
    }

    def "transform value returns an either with transformed value"() {
        given:
        Function<String, Character[]> function = (String it) -> it.toCharArray();
        when:
        var result = valueObject.transformValue(function)
        then:
        result.get() == function.apply(valueObject.get())
    }

    def "transform error returns an either with unchanged error"() {

    }
}
