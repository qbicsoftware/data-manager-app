package life.qbic.identity.infrastructure.broadcasting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.communication.broadcasting.IntegrationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * <b>Message Dispatcher</b>
 * <p>
 * Implementation of the {@link EventHub} interface, broadcasting events to the messaging
 * middleware.
 *
 * @since 1.0.0
 */
@Component
public class MessageDispatcher implements EventHub {

  @Value("${qbic.broadcasting.identity.topic}")
  private static String IDENTITY_TOPIC;
  private final JmsTemplate jmsTemplate;

  @Autowired
  public MessageDispatcher(JmsTemplate jmsTemplate) {
    this.jmsTemplate = Objects.requireNonNull(jmsTemplate);
  }

  @Override
  public void send(IntegrationEvent event) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      jmsTemplate.convertAndSend(IDENTITY_TOPIC, mapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Event broadcasting failed!", e);
    }
  }
}
