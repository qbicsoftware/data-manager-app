package life.qbic.projectmanagement.infrastructure.communication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * <b>Message Consumer</b>
 * <p>
 * Consumes incoming messages from a message broker, converts the content into a POJO
 * ({@link IntegrationEvent}) and forwards the event to the local message router.
 *
 * @since 1.0.0
 */
@Component
public class MessageConsumer {

  private static final Logger log = logger(MessageConsumer.class);

  private final MessageRouter messageRouter;

  @Autowired
  public MessageConsumer(MessageRouter messageRouter) {
    this.messageRouter = messageRouter;
    log.debug("Created project management message consumer");
  }

  @JmsListener(destination = "${qbic.broadcasting.identity.topic}")
  public void listenToMessageBroker(String content) {
    log.debug("Incoming message with content: %s".formatted(content));
    messageRouter.dispatch(parse(content));
  }

  private IntegrationEvent parse(String content) {
    var objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(content, IntegrationEvent.class);
    } catch (JsonProcessingException e) {
      log.error("Json to object mapping failed!", e);
      throw new IllegalArgumentException("Content does not seem to be an integration event.");
    }
  }
}
