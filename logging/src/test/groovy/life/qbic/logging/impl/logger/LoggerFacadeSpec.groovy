package life.qbic.logging.impl.logger

import life.qbic.logging.api.LogMessage
import life.qbic.logging.api.Publisher
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

    def "When an error message is logged, all subscribers are notified and the message is contained in the log"()```
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

    def "When a debug message is logged, all subscribers are notified and the message is contained in the log"() {
    ```
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

}
