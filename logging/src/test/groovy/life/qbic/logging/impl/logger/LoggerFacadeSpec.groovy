package life.qbic.logging.impl.logger

import life.qbic.logging.api.Publisher
import life.qbic.logging.subscription.api.LogMessage
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class LoggerFacadeSpec extends Specification {

    def "When an info message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def publisher = Mock(Publisher.class)

        and:
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.info("test")

        then:
        1 * publisher.publish(_ as LogMessage)
        byteOutputStream.toString().contains("test")

    }

    def "When an error message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def publisher = Mock(Publisher.class)

        and:
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.error("test")

        then:
        1 * publisher.publish(_ as LogMessage)
        byteOutputStream.toString().contains("test")

    }

    def "When a error message with throwable cause is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def publisher = Mock(Publisher.class)

        and:
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.error("test", new Throwable("out of coffee"))

        then:
        1 * publisher.publish(_ as LogMessage)
        byteOutputStream.toString().contains("test") && byteOutputStream.toString().contains("out of coffee")

    }


    def "When a debug message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def publisher = Mock(Publisher.class)

        and:
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.debug("test")

        then:
        1 * publisher.publish(_ as LogMessage)
        byteOutputStream.toString().contains("test")

    }

    def "When a debug message with throwable cause is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def publisher = Mock(Publisher.class)

        and:
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.debug("test", new Throwable("out of coffee"))

        then:
        1 * publisher.publish(_ as LogMessage)
        byteOutputStream.toString().contains("test") && byteOutputStream.toString().contains("out of coffee")

    }

    def "When a warning message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def publisher = Mock(Publisher.class)

        and:
        def byteOutputStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteOutputStream))

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.warn("test")

        then:
        1 * publisher.publish(_ as LogMessage)
        byteOutputStream.toString().contains("test")

    }

}
