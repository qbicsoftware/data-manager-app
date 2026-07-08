package life.qbic.logging.impl.logger

import life.qbic.logging.api.Publisher
import life.qbic.logging.subscription.api.LogMessage
import spock.lang.Specification

class LoggerFacadeSpec extends Specification {

    def "When an info message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def captor = new MessageCaptor()
        def publisher = Stub(Publisher.class) {
            publish(_) >> { captor.capture(it) }
        }

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.info("test")

        then:
        captor.message == "test"

    }

    def "When an error message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def captor = new MessageCaptor()
        def publisher = Stub(Publisher.class) {
            publish(_) >> { captor.capture(it) }
        }

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.error("test")

        then:
        captor.message == "test"

    }

    def "When a error message with throwable cause is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def captor = new MessageCaptor()
        def publisher = Stub(Publisher.class) {
            publish(_) >> { captor.capture(it) }
        }

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.error("test", new Throwable("out of coffee"))

        then:
        captor.message == "test"
        captor.cause?.message == "out of coffee"

    }


    def "When a debug message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def captor = new MessageCaptor()
        def publisher = Stub(Publisher.class) {
            publish(_) >> { captor.capture(it) }
        }

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.debug("test")

        then:
        captor.message == "test"

    }

    def "When a debug message with throwable cause is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def captor = new MessageCaptor()
        def publisher = Stub(Publisher.class) {
            publish(_) >> { captor.capture(it) }
        }

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.debug("test", new Throwable("out of coffee"))

        then:
        captor.message == "test"
        captor.cause?.message == "out of coffee"

    }

    def "When a warning message is logged, all subscribers are notified and the message is contained in the log"() {
        given:
        def captor = new MessageCaptor()
        def publisher = Stub(Publisher.class) {
            publish(_) >> { captor.capture(it) }
        }

        and:
        def logger = LoggerFacade.from(LoggerFacadeSpec.class, publisher)

        when:
        logger.warn("test")

        then:
        captor.message == "test"

    }

    static class MessageCaptor {
        String message
        Throwable cause

        void capture(LogMessage msg) {
            if (msg != null) {
                message = msg.message
                cause = msg.cause
            }
        }
    }

}
