package life.qbic.logging.impl.publisher

import life.qbic.logging.subscription.api.LogLevel
import life.qbic.logging.subscription.api.LogMessage
import life.qbic.logging.subscription.api.Subscriber
import spock.lang.Specification

import java.nio.charset.Charset

class SimplePublisherSpec extends Specification {

    def "The publisher informs all subscriber about new incoming log messages"() {
        given:
        def byteStream = new ByteArrayOutputStream()
        System.setOut(new PrintStream(byteStream))

        def publisher = new SimplePublisher()
        def subscriberOne = new Subscriber() {
            @Override
            void onMessageArrived(LogMessage logMessage) {
                byteStream.write(logMessage.message().getBytes(Charset.defaultCharset()))
            }
        }
        publisher.subscribe(subscriberOne)

        and:
        def subscriberTwo = new Subscriber() {
            @Override
            void onMessageArrived(LogMessage logMessage) {
                println logMessage.message()
            }
        }
        publisher.subscribe(subscriberTwo)

        when:
        publisher.publish(new LogMessage("SpockTest", LogLevel.INFO, "message", null))
        // Because the submission works concurrently in an own thread,
        // we need to anticipate a small delay until the
        // subscribers write to the output stream
        Thread.sleep(1000)

        then:
        byteStream.toString().count("message") == 2

    }


}
