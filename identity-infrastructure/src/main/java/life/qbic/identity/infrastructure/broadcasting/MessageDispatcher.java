package life.qbic.identity.infrastructure.broadcasting;

import java.util.Objects;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.communication.broadcasting.IntegrationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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

  private static String IDENTITY_TOPIC;
  private final JmsTemplate jmsTemplate;

  @Autowired
  public MessageDispatcher(JmsTemplate jmsTemplate,
      @Value("${qbic.broadcasting.identity.topic}") String topic) {
    this.jmsTemplate = Objects.requireNonNull(jmsTemplate);
    IDENTITY_TOPIC = topic;
  }

  @Override
  public void send(IntegrationEvent event) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      jmsTemplate.convertAndSend(IDENTITY_TOPIC, mapper.writeValueAsString(event));
    } catch (JacksonException e) {
      throw new RuntimeException("Event broadcasting failed!", e);
    }
  }
}
