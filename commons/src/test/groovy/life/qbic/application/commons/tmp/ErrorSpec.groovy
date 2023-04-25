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
    static Result<Integer, String> errorObject = Result.fromError("Oh no, an error!")

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
        Function<Integer, Long> function = (Integer it) -> (long) it
        when:
        var result = errorObject.map(function)
        then:
        result.get() == errorObject.get()
    }

    def "transform error returns an either with transformed error"() {
        given:
        Function<String, Long> function = (String it) -> it.length()
        when:
        var result = errorObject.mapError(function)
        then:
        result.get() == function.apply(errorObject.get())
    }

    def "either containing a value is equal to another either containing the value"() {
        given:
        Result<Integer, String> e1 = Result.fromError("hello")
        Result<Integer, String> e2 = Result.fromError("hello")
        expect:
        e1.equals(e2)
    }

    def "either containing a value is not equal to another either containing a different value"() {
        given:
        Result<Integer, String> e1 = Result.fromError("hello")
        Result<Integer, String> e2 = Result.fromError("hello2")
        expect:
        e1 != e2
    }

    def "bind value returns an either with the same error"() {
        given:
        Function<Integer, Result<String, Integer>> mapper = (Integer it) -> Result.fromValue("bla")
        when:
        var result = errorObject.flatMap(mapper)

        then:
        result.get() == errorObject.get()
    }

    def "bind error returns an either with the mapped error"() {
        given:
        Function<String, Result<Integer, Integer>> mapper = (String it) -> Result.<Integer, Integer> fromError(it.length())
        when:
        var result = errorObject.flatMapError(mapper)
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

    def "recover returns an either with mapped value object"() {
        given:
        Function<String, Integer> function = it -> it.length()
        when:
        var result = errorObject.recover(function)
        then:
        result.isValue()
        result.get() == function.apply(errorObject.get())
    }

    def "valueOrElse returns else"() {
        expect:
        42 == errorObject.valueOrElse(42)
    }

    def "valueOrElseGet returns the supplier's value"() {
        expect:
        42 == errorObject.valueOrElseGet(() -> 42)
    }

    def "valueOrElseThrow throws the supplied throwable"() {
        given:
        RuntimeException expectedException = new RuntimeException("Oha! No value!")
        when:
        errorObject.valueOrElseThrow(() -> expectedException)
        then:
        def e = thrown(RuntimeException)
        e == expectedException
    }
}
