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
        given:
        Function<Integer, Short> function = (Integer it) -> it.shortValue();
        when:
        var result = valueObject.transformError(function)
        then:
        result.get() == valueObject.get()
    }

    def "either containing a value is equal to another either containing the value"() {
        given:
        Either<String, Integer> e1 = Either.fromValue("hello")
        Either<String, Integer> e2 = Either.fromValue("hello")
        expect:
        e1.equals(e2)
    }

    def "either containing a value is not equal to another either containing a different value"() {
        given:
        Either<String, Integer> e1 = Either.fromValue("hello")
        Either<String, Integer> e2 = Either.fromValue("hello2")
        expect:
        e1 != e2
    }

    def "bind value returns an either with the mapped value"() {
        given:
        Function<String, Either<Integer, Integer>> mapper = (String it) -> Either.<Integer, Integer> fromValue(it.length())
        when:
        var result = valueObject.bindValue(mapper)
        then:
        result == mapper.apply(valueObject.get())
    }

    def "bind error returns an either with unchanged error"() {
        given:
        Function<Integer, Either<Integer, Integer>> mapper = (Integer it) -> Either.fromValue(5)
        when:
        var result = valueObject.bindError(mapper)

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

    def "valueOrElse retuns the value"() {
        expect:
        valueObject.get() == valueObject.valueOrElse("my other Value")
    }
}
