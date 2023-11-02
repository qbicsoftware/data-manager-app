package life.qbic.projectmanagement.infrastructure.communication;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class MessageConsumer {

  private static final Logger log = logger(MessageConsumer.class);

  private final MessageRouter messageRouter;

  @Autowired
  public MessageConsumer(MessageRouter messageRouter) {
    this.messageRouter = messageRouter;
    log.info("Build Message Consumer");
  }

  @JmsListener(destination = "User")
  public void listenToMessageBroker(String content) {
    log.debug("Incoming message with content: %s".formatted(content));
    messageRouter.dispatch(parse(content));
  }

  private IntegrationEvent parse(String content) throws RuntimeException {
    var objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(content, IntegrationEvent.class);
    } catch (JsonProcessingException e) {
      log.error("Json to object mapping failed!", e);
      throw new RuntimeException("Content does not seem to be an integration event.");
    }
  }
}
