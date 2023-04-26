package life.qbic.application.commons

import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function

import static life.qbic.application.commons.Result.InvalidResultAccessException
import static life.qbic.application.commons.Result.fromValue

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class ValueSpec extends Specification {

    static Result<String, Integer> valueObject = fromValue("test")

    def "on value calls the consumer"() {
        given:
        var value = fromValue("test")
        Consumer<String> consumer = Mock()
        when:
        def result = value.onValue(consumer)
        then:
        1 * consumer.accept(_)
        result.is(value)
    }

    def "on value matching calls the consumer if predicate matches"() {
        given:
        var value = fromValue("test")
        Consumer<String> consumer = Mock()
        when:
        def result = value.onValueMatching({ true }, consumer)
        then:
        1 * consumer.accept(_)
        result.is(value)
    }

    def "on value matching does not call the consumer if predicate does not match"() {
        given:
        var value = fromValue("test")
        Consumer<String> consumer = Mock()
        when:
        def result = value.onValueMatching({ false }, consumer)
        then:
        0 * consumer.accept(_)
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

    def "on error matching (#predicateResult) does not call the consumer"() {
        given:
        Consumer<Integer> consumer = Mock()
        when:
        def result = valueObject.onErrorMatching({ predicateResult }, consumer)
        then:
        0 * consumer.accept(_)
        result.is(valueObject)
        where:
        predicateResult << [true, false]
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
        Function<String, Character[]> function = (String it) -> it.toCharArray()
        when:
        var result = valueObject.map(function)
        then:
        result.get() == function.apply(valueObject.get())
    }

    def "transform error returns an either with unchanged error"() {
        given:
        Function<Integer, Short> function = (Integer it) -> it.shortValue()
        when:
        var result = valueObject.mapError(function)
        then:
        result.get() == valueObject.get()
    }

    def "either containing a value is equal to another either containing the value"() {
        given:
        Result<String, Integer> e1 = fromValue("hello")
        Result<String, Integer> e2 = fromValue("hello")
        expect:
        e1.equals(e2)
    }

    def "either containing a value is not equal to another either containing a different value"() {
        given:
        Result<String, Integer> e1 = fromValue("hello")
        Result<String, Integer> e2 = fromValue("hello2")
        expect:
        e1 != e2
    }

    def "bind value returns an either with the mapped value"() {
        given:
        Function<String, Result<Integer, Integer>> mapper = (String it) -> Result.<Integer, Integer> fromValue(it.length())
        when:
        var result = valueObject.flatMap(mapper)
        then:
        result == mapper.apply(valueObject.get())
    }

    def "bind error returns an either with unchanged error"() {
        given:
        Function<Integer, Result<Integer, Integer>> mapper = (Integer it) -> fromValue(5)
        when:
        var result = valueObject.flatMapError(mapper)

        then:
        result.get() == valueObject.get()
    }

    def "fold returns the mapped value"() {
        expect:
        42 == valueObject.fold(
                value -> 42,
                error -> 0
        )
    }

    def "recover returns this"() {
        expect:
        valueObject == valueObject.recover(it -> "test")
    }

    def "getValue returns the value"() {
        expect:
        valueObject.get() == valueObject.getValue()
    }

    def "valueOrElse returns the value"() {
        expect:
        valueObject.get() == valueObject.valueOrElse("my other Value")
    }

    def "valueOrElseGet returns the value"() {
        expect:
        valueObject.get() == valueObject.valueOrElseGet(() -> "my other value")
    }

    def "valueOrElseThrow returns the value"() {
        expect:
        valueObject.get() == valueObject.valueOrElseThrow(() -> new RuntimeException("Oha! No value!"))
    }

    def "getError throws an exception"() {
        when:
        valueObject.getError()
        then:
        thrown(InvalidResultAccessException)
    }
}
