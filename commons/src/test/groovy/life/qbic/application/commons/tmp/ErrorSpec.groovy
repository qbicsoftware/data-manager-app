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
class ErrorSpec extends Specification {
    static Either<Integer, String> errorObject = Either.fromError("Oh no, an error!")

    def "on value returns this"() {
        given:
        Consumer<Integer> consumer = Mock()
        when:
        def result = errorObject.onValue(consumer)
        then:
        0 * consumer.accept(_)
        result.is(errorObject)
    }

    def "on error calls the consumer"() {
        given:
        Consumer<String> consumer = Mock()
        when:
        def result = errorObject.onError(consumer)
        then:
        1 * consumer.accept(_)
        result.is(errorObject)
    }

    def "is value returns false"() {
        expect:
        !errorObject.isValue()
    }

    def "is error returns true"() {
        expect:
        errorObject.isError()
    }

    def "transform value returns an either with unchanged value"() {
        given:
        Function<Integer, Long> function = (Integer it) -> (long) it;
        when:
        var result = errorObject.transformValue(function)
        then:
        result.get() == errorObject.get()
    }

    def "transform error returns an either with transformed error"() {
        given:
        Function<String, Long> function = (String it) -> it.length();
        when:
        var result = errorObject.transformError(function)
        then:
        result.get() == function.apply(errorObject.get())
    }

    def "bind value returns an either with the same value"() {
        expect:
        false
    }

    def "bind error returns an either with the mapped error"() {
        expect:
        false
    }
}
