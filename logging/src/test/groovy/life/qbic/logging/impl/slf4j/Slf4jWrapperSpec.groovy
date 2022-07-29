package life.qbic.logging.impl.slf4j

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class Slf4jWrapperSpec extends Specification {

    def "logging on info level is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def infoMessage = "Info message"

        when:
        wrapper.info(infoMessage)

        then:
        byteOutputStream.toString().contains(infoMessage)
    }

    def "logging on error level is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def errorMessage = "Error message"

        when:
        wrapper.error(errorMessage)

        then:
        byteOutputStream.toString().contains(errorMessage)
    }

    def "logging on debug level is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def debugMessage = "Debug message"

        when:
        wrapper.error(debugMessage)

        then:
        byteOutputStream.toString().contains(debugMessage)
    }

    def "logging on error level with throwable is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def errorMessage = "Error message"
        def throwable = new Throwable("Out of coffee")

        when:
        wrapper.error(errorMessage, throwable)

        then:
        byteOutputStream.toString().contains(errorMessage) && byteOutputStream.toString().contains(throwable.toString())
    }

}
