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

    def "either containing a value is equal to another either containing the value"() {
        given:
        Either<Integer, String> e1 = Either.fromError("hello")
        Either<Integer, String> e2 = Either.fromError("hello")
        expect:
        e1.equals(e2)
    }

    def "either containing a value is not equal to another either containing a different value"() {
        given:
        Either<Integer, String> e1 = Either.fromError("hello")
        Either<Integer, String> e2 = Either.fromError("hello2")
        expect:
        e1 != e2
    }

    def "bind value returns an either with the same error"() {
        given:
        Function<Integer, Either<String, Integer>> mapper = (Integer it) -> Either.fromValue("bla")
        when:
        var result = errorObject.bindValue(mapper)

        then:
        result.get() == errorObject.get()
    }

    def "bind error returns an either with the mapped error"() {
        given:
        Function<String, Either<Integer, Integer>> mapper = (String it) -> Either.<Integer, Integer> fromError(it.length())
        when:
        var result = errorObject.bindError(mapper)
        then:
        result == mapper.apply(errorObject.get())
    }

    def "fold returns the mapped error"() {
        expect:
        0 == errorObject.fold(
                value -> 42,
                error -> 0
        )
    }
}
