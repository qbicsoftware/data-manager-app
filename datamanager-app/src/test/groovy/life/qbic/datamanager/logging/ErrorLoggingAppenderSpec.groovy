package life.qbic.datamanager.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.net.SMTPAppender
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Layout
import spock.lang.Specification

/**
 * End-to-end verification that the error-notification e-mail appender renders only the
 * timestamp and the exception class (no log message, no stack trace), so no personal data
 * is transmitted in the e-mail.
 *
 * <p>Loads a logback config that mirrors the production {@code EmailOnError} appender
 * (including the {@code errorClass} conversion word) and inspects the rendered layout.
 */
class ErrorLoggingAppenderSpec extends Specification {

    private static final String CONFIG = "/logback-error-appender-test.xml"

    private LoggerContext context
    private Layout layout

    def setup() {
        context = new LoggerContext()
        def configurator = new JoranConfigurator()
        configurator.setContext(context)
        context.reset()
        configurator.doConfigure(this.getClass().getResourceAsStream(CONFIG))

        SMTPAppender appender = context.getLogger("root").getAppender("EmailOnError")
        assert appender != null : "EmailOnError appender was not configured"
        layout = appender.layout
    }

    def "renders only the timestamp and error class for an error with personal data"() {
        given: "an ERROR event whose message contains personal data and a throwable"
        def logger = context.getLogger("life.qbic.datamanager.sample.OpenbisConnector")
        def event = new LoggingEvent(
                "life.qbic.datamanager.sample.OpenbisConnector",
                logger,
                Level.ERROR,
                "failed for user jane.doe@example.com token abc123 sample QBCI-12345",
                new NullPointerException("boom for sample QBCI-12345"),
                null)

        when:
        def rendered = layout.doLayout(event)

        then: "the e-mail contains a timestamp"
        rendered.matches("(?s).*\\d{4}-\\d{2}-\\d{2}.*")

        and: "it contains the exception class"
        rendered.contains("java.lang.NullPointerException")

        and: "it does NOT contain the log message or personal data"
        !rendered.contains("jane.doe@example.com")
        !rendered.contains("abc123")
        !rendered.contains("QBCI-12345")
        !rendered.contains("failed for user")

        and: "it does NOT contain stack trace frames"
        !rendered.contains("at life.qbic.datamanager")
        !rendered.contains("ErrorLoggingAppenderSpec")
    }
}