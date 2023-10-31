package life.qbic.identity.infrastructure.broadcasting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.communication.broadcasting.IntegrationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class MessageDispatcher implements EventHub {

  private final JmsTemplate jmsTemplate;

  private static final String QUEUE = "User";

  @Autowired
  public MessageDispatcher(JmsTemplate jmsTemplate) {
    this.jmsTemplate = Objects.requireNonNull(jmsTemplate);
  }

  @Override
  public void send(IntegrationEvent event) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      jmsTemplate.convertAndSend(QUEUE, mapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Event broadcasting failed!", e);
    }
  }
}
