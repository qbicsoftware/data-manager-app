package life.qbic.application.commons


import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function

class ResultSpec extends Specification {

    def "A result shall throw an NPE, when the value parameter is null"() {
        when:
        String s = null
        Result<String, Exception> result = Result.success(s)

        then:
        thrown(NullPointerException)
    }

    def "A result shall throw an NPE, when the error parameter is null"() {
        when:
        Exception e = null
        Result<String, Exception> result = Result.failure(e)

        then:
        thrown(NullPointerException)
    }

    def "When a result contains a value, apple the provided function"() {
        given:
        Function<String, String> toUpperCase = s -> {
            return s.toUpperCase()
        }

        and:
        String word = "automobile"
        Result<String, Exception> result = Result.success(word)

        when:
        Result<String, Exception> processed = result.map(toUpperCase)

        then:
        processed.isSuccess()
        processed.value() == word.toUpperCase()
    }

    def "When a result contains an exception, dont apply the provided function but return the original result"() {
        given:
        Function<String, String> toUpperCase = s -> {
            return s.toUpperCase()
        }

        and:
        def result = Result.failure(new RuntimeException("test exception"))

        when:
        def processedResult = result.map(toUpperCase)

        then:
        processedResult.isFailure()
        processedResult.exception().message == "test exception"
    }

    def "When a result contains a value, pass the value to the consumer"() {
        given:
        Consumer<String> consumer = Mock(Consumer)

        and:
        String value = "test value"

        and:
        Result<String, Exception> result = Result.success(value)

        when:
        result.ifSuccess(consumer)

        then:
        1 * consumer.accept(_)
    }

    def "When a result contains an exception, dont call a provided consumer"() {
        given:
        Consumer<String> consumer = Mock(Consumer)

        and:
        Exception e = new RuntimeException("test")

        and:
        Result<String, Exception> result = Result.failure(e)

        when:
        result.ifSuccess(consumer)

        then:
        0 * consumer.accept(_)
    }

    def "When a result contains an exception, call a provided error consumer"() {
        given:
        Consumer<String> consumer = Mock(Consumer)

        and:
        Exception e = new RuntimeException("test")

        and:
        Result<String, Exception> result = Result.failure(e)

        when:
        result.ifFailure(consumer)

        then:
        1 * consumer.accept(_)
    }

    def "When a result contains a value, dont call a provided error consumer"() {
        given:
        Consumer<String> consumer = Mock(Consumer)

        and:
        String value = "Test"

        and:
        Result<String, Exception> result = Result.success(value)

        when:
        result.ifFailure(consumer)

        then:
        0 * consumer.accept(_)
    }

    def "When a result contains a value, call the success consumer and not the error consumer"() {
        given:
        Consumer<String> successConsumer = Mock(Consumer)
        Consumer<String> errorConsumer = Mock(Consumer)

        and:
        String value = "Test"

        and:
        Result<String, Exception> result = Result.success(value)

        when:
        result.ifSuccessOrElse(successConsumer, errorConsumer)

        then:
        1 * successConsumer.accept(_)
        0 * errorConsumer.accept(_)
    }

    def "When a result contains an exception, call the error consumer and not the error consumer"() {
        given:
        Consumer<String> successConsumer = Mock(Consumer)
        Consumer<String> errorConsumer = Mock(Consumer)

        and:
        Exception e = new RuntimeException("test")

        and:
        Result<String, Exception> result = Result.failure(e)

        when:
        result.ifSuccessOrElse(successConsumer, errorConsumer)

        then:
        0 * successConsumer.accept(_)
        1 * errorConsumer.accept(_)
    }

    def "When a result contains an exception, call the exception supplier"() {
        given:
        def exceptionSupplier = { return new RuntimeException("o.O") }

        and:
        Exception e = new RuntimeException("error")

        and:
        Result<String, Exception> result = Result.failure(e)

        when:
        result.orElseThrow(exceptionSupplier)

        then:
        def runtimeException = thrown(RuntimeException)
        runtimeException.message == "o.O"
    }

    def "When a result contains a value, return the value without calling the exception supplier"() {
        given:
        def exceptionSupplier = { return new RuntimeException("o.O") }

        and:
        String value = "Result value"

        and:
        Result<String, Exception> result = Result.success(value)

        when:
        String passedValue = result.orElseThrow(exceptionSupplier)

        then:
        noExceptionThrown()
        passedValue == value
    }

}
