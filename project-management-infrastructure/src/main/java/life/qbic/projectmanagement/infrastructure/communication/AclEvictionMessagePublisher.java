package life.qbic.projectmanagement.infrastructure.communication;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Map;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.AclEvictionPublisher;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * <b>ACL eviction message publisher</b>
 *
 * <p>Publishes an {@link IntegrationEvent} of type {@code aclCacheEvicted} on the dedicated
 * eviction topic whenever the ACL of a project changed. The event is consumed by every running
 * instance (pub-sub, {@code spring.jms.pub-sub-domain=true}), which then evicts the project's
 * entry from its process-local {@code acl_cache} (≤60s revocation NFR, strategy §4.6).</p>
 *
 * <p>Follows the {@code MessageDispatcher} precedent from the identity context (topic injected via
 * {@code @Value}, JSON serialization with the tools.jackson mapper).</p>
 *
 * @since 1.0.0
 */
@Component
public class AclEvictionMessagePublisher implements AclEvictionPublisher {

  private static final Logger log = logger(AclEvictionMessagePublisher.class);

  public static final String ACL_EVICTION_EVENT_TYPE = "aclCacheEvicted";

  private final JmsTemplate jmsTemplate;

  private final String topic;

  @Autowired
  public AclEvictionMessagePublisher(JmsTemplate jmsTemplate,
      @Value("${qbic.broadcasting.acl-eviction.topic}") String topic) {
    this.jmsTemplate = Objects.requireNonNull(jmsTemplate);
    this.topic = topic;
  }

  @Override
  public void publishAclEviction(ProjectId projectId) {
    ObjectMapper mapper = new ObjectMapper();
    IntegrationEvent event = IntegrationEvent.create(ACL_EVICTION_EVENT_TYPE,
        Map.of("projectId", projectId.value()));
    try {
      jmsTemplate.convertAndSend(topic, mapper.writeValueAsString(event));
      log.debug("Published ACL eviction event for project %s on topic %s".formatted(
          projectId.value(), topic));
    } catch (JacksonException e) {
      throw new RuntimeException("ACL eviction broadcasting failed!", e);
    }
  }
}