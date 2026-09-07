package life.qbic.datamanager.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * Verifies that {@link ErrorClassConverter} renders only the exception class name,
 * without the log message or the stack trace, so that no personal data is included
 * in the error-notification email.
 */
class ErrorClassConverterSpec extends Specification {

    private ErrorClassConverter converter
    private LoggerContext context
    private Logger logger

    def setup() {
        context = (LoggerContext) LoggerFactory.getILoggerFactory()
        logger = context.getLogger("life.qbic.datamanager.Application")
        converter = new ErrorClassConverter()
        converter.setContext(context)
        converter.start()
    }

    def "renders only the exception class name for an event with a throwable"() {
        given:
        def event = eventWithThrowable(new NullPointerException("boom for sample QBCI-12345"))

        when:
        def result = converter.convert(event)

        then:
        result == "java.lang.NullPointerException"
        !result.contains("boom for sample")      // no message
        !result.contains("ErrorClassConverterSpec") // no stack trace frames
    }

    def "renders an empty string when the event has no throwable"() {
        given:
        def event = new LoggingEvent(
                "life.qbic.datamanager.Application",
                logger,
                Level.ERROR,
                "a log message without a throwable",
                null,
                null)

        expect:
        converter.convert(event).isEmpty()
    }

    private static LoggingEvent eventWithThrowable(Throwable throwable) {
        def ctx = (LoggerContext) LoggerFactory.getILoggerFactory()
        def logger = ctx.getLogger("life.qbic.datamanager.Application")
        // the LoggingEvent constructor sets the throwable proxy itself
        return new LoggingEvent(
                "life.qbic.datamanager.Application",
                logger,
                Level.ERROR,
                "secret message jane@example.com",
                throwable,
                null)
    }
}