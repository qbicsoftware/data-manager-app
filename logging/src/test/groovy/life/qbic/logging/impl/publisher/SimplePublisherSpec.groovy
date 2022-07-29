package life.qbic.logging.impl.publisher

import life.qbic.logging.api.LogLevel
import life.qbic.logging.api.LogMessage
import life.qbic.logging.api.Subscriber
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class SimplePublisherSpec extends Specification {

    def "The publisher informs all subscriber about new incoming log messages"() {
        given:
        def byteStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteStream))

        def publisher = new SimplePublisher()
        def subscriberOne = new Subscriber() {
            @Override
            void onNewMessage(LogMessage logMessage) {
                println logMessage.message()
            }
        }
        publisher.subscribe(subscriberOne)

        and:
        def subscriberTwo = new Subscriber() {
            @Override
            void onNewMessage(LogMessage logMessage) {
                println logMessage.message()
            }
        }
        publisher.subscribe(subscriberTwo)

        when:
        publisher.publish(new LogMessage("SpockTest", LogLevel.INFO, "message", null))

        then:
        byteStream.toString().count("message") == 2

    }



}
