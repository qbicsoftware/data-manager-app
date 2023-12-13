package life.qbic.application.commons


import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function

import static life.qbic.application.commons.Result.*


class ErrorSpec extends Specification {
    static Result<Integer, String> errorObject = fromError("Oh no, an error!")

    def "on value returns this"() {
        given:
        Consumer<Integer> consumer = Mock()
        when:
        def result = errorObject.onValue(consumer)
        then:
        0 * consumer.accept(_)
        result.is(errorObject)
    }

    def "on value matching (#predicateEvaluated) does not call the consumer"() {
        given:
        Consumer<Integer> consumer = Mock()
        when:
        def result = errorObject.onValueMatching({ predicateEvaluated }, consumer)
        then:
        0 * consumer.accept(_)
        result.is(errorObject)
        where:
        predicateEvaluated << [true, false]
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

    def "on error matching calls the consumer if the predicate matches"() {
        given:
        Consumer<String> consumer = Mock()
        when:
        def result = errorObject.onErrorMatching({ true }, consumer)
        then:
        1 * consumer.accept(_)
        result.is(errorObject)
    }

    def "on error matching does not call the consumer if the predicate does not match"() {
        given:
        Consumer<String> consumer = Mock()
        when:
        def result = errorObject.onErrorMatching({ false }, consumer)
        then:
        0 * consumer.accept(_)
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
        result.value == errorObject.value
    }

    def "transform error returns an either with transformed error"() {
        given:
        Function<String, Long> function = (String it) -> it.length()
        when:
        var result = errorObject.mapError(function)
        then:
        result.getError() == function.apply(errorObject.getError())
    }

    def "either containing a value is equal to another either containing the value"() {
        given:
        Result<Integer, String> e1 = fromError("hello")
        Result<Integer, String> e2 = fromError("hello")
        expect:
        e1.equals(e2)
    }

    def "either containing a value is not equal to another either containing a different value"() {
        given:
        Result<Integer, String> e1 = fromError("hello")
        Result<Integer, String> e2 = fromError("hello2")
        expect:
        e1 != e2
    }

    def "bind value returns an either with the same error"() {
        given:
        Function<Integer, Result<String, Integer>> mapper = (Integer it) -> fromValue("bla")
        when:
        var result = errorObject.flatMap(mapper)

        then:
        result.getError() == errorObject.getError()
    }

    def "bind error returns an either with the mapped error"() {
        given:
        Function<String, Result<Integer, Integer>> mapper = (String it) -> fromError(it.length())
        when:
        var result = errorObject.flatMapError(mapper)
        then:
        result == mapper.apply(errorObject.getError())
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
        result.valueOrElseThrow(() -> new RuntimeException("Test")) == function.apply(errorObject.getError())
    }

    def "getValue throws an exception"() {
        when:
        errorObject.getValue()
        then:
        thrown(InvalidResultAccessException)
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
