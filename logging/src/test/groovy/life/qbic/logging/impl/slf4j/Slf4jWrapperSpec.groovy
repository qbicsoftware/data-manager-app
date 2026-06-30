package life.qbic.logging.impl.slf4j

import spock.lang.Specification

class Slf4jWrapperSpec extends Specification {

    def "logging on info level is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def infoMessage = "Info message"

        when:
        wrapper.info(infoMessage)

        then:
        noExceptionThrown()
    }

    def "logging on error level is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def errorMessage = "Error message"

        when:
        wrapper.error(errorMessage)

        then:
        noExceptionThrown()
    }

    def "logging on debug level is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def debugMessage = "Debug message"

        when:
        wrapper.error(debugMessage)

        then:
        noExceptionThrown()
    }

    def "logging on error level with throwable is redirected to the slf4j api and its binder"() {
        given:
        def wrapper = Slf4jWrapper.create(Object.class)
        def errorMessage = "Error message"
        def throwable = new Throwable("Out of coffee")

        when:
        wrapper.error(errorMessage, throwable)

        then:
        noExceptionThrown()
    }

}
