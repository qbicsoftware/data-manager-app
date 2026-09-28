package life.qbic.projectmanagement.infrastructure.communication

import life.qbic.projectmanagement.domain.model.project.ProjectId
import org.springframework.jms.core.JmsTemplate
import spock.lang.Specification

/**
 * Unit tests for the {@link AclEvictionMessagePublisher}.
 *
 * <p>Verifies that a project ACL change is broadcast as an {@code aclCacheEvicted} integration
 * event carrying the project id on the configured eviction topic (plan D4, ≤60s revocation NFR).</p>
 */
class AclEvictionPublisherSpec extends Specification {

  static final String TOPIC = "ProjectAccessAclEvictions"

  JmsTemplate jmsTemplate = Mock()
  AclEvictionMessagePublisher publisher

  def setup() {
    publisher = new AclEvictionMessagePublisher(jmsTemplate, TOPIC)
  }

  def "publishes an aclCacheEvicted event with the project id on the eviction topic"() {
    given: "a project whose ACL changed"
    ProjectId projectId = ProjectId.parse("9f7a6e3e-2d1e-4a6b-8c5d-9a0b1c2d3e4f")

    when: "the eviction is published"
    publisher.publishAclEviction(projectId)

    then: "the event is sent to the configured topic with the project id in its payload"
    1 * jmsTemplate.convertAndSend(TOPIC, _ as String) >> { String topic, String payload ->
      assert topic == TOPIC
      assert payload.contains('"aclCacheEvicted"')
      assert payload.contains(projectId.value())
    }
  }

  def "does not swallow send failures"() {
    given: "a project whose ACL changed"
    ProjectId projectId = ProjectId.create()

    when: "sending to the broker fails"
    publisher.publishAclEviction(projectId)

    then: "the failure propagates instead of being swallowed"
    1 * jmsTemplate.convertAndSend(TOPIC, _ as String) >> { throw new RuntimeException("convert failed") }
    thrown(RuntimeException)
  }
}