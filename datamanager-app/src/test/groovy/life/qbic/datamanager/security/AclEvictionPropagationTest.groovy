package life.qbic.datamanager.security

import life.qbic.projectmanagement.application.authorization.acl.AclEvictionPublisher
import life.qbic.projectmanagement.domain.model.project.Project
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.infrastructure.communication.AclEvictionMessagePublisher
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import org.apache.activemq.artemis.jms.server.config.impl.ConnectionFactoryConfigurationImpl
import org.apache.activemq.artemis.jms.server.config.impl.JMSConfigurationImpl
import org.apache.activemq.artemis.api.core.SimpleString
import org.apache.activemq.artemis.core.server.Queue
import org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerEndpointRegistry
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.support.converter.SimpleMessageConverter
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.model.AclCache
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.ObjectIdentity
import spock.lang.Specification
import spock.lang.Timeout

import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * End-to-end proof of the cross-instance ACL cache eviction broadcast (plan D4).
 *
 * <p>Starts a real embedded Artemis broker (same-JVM {@code vm://0} transport), connects the
 * production {@link AclEvictionMessagePublisher} (via a Spring {@link JmsTemplate}) and registers
 * the production {@link AclCacheEvictionListener} through a minimal Spring context wired with
 * {@code @EnableJms}. Publishing an eviction for a project must trigger the listener to evict that
 * project's {@link ObjectIdentity} from the ACL cache on the receiving side.</p>
 *
 * <p>The ≤60s revocation NFR (strategy §4.6) is asserted with a far tighter wall-clock bound;
 * on a healthy LAN the broker hop is sub-second.</p>
 *
 * <p>Run with {@code ./mvnw -pl datamanager-app test -Dtest=AclEvictionPropagationTest}
 * (no external broker or database required — the embedded broker is started and stopped within
 * this test).</p>
 */
@Timeout(60)
class AclEvictionPropagationTest extends Specification {

  static final String TOPIC = "ProjectAccessAclEvictions"

  EmbeddedJMS broker
  AnnotationConfigApplicationContext context

  def setup() {
    // Start an embedded Artemis broker with an in-VM transport (same JVM, no TCP allocation).
    ConfigurationImpl configuration = new ConfigurationImpl()
        .setPersistenceEnabled(false)
        .setSecurityEnabled(false)
        .setJournalDirectory(createTempDirectory("artemis-journal"))
        .setBindingsDirectory(createTempDirectory("artemis-bindings"))
        .setLargeMessagesDirectory(createTempDirectory("artemis-large"))
        .setPagingDirectory(createTempDirectory("artemis-paging"))
        .setName("acl-eviction-it-broker")
    configuration.addConnectorConfiguration("invm-connector", "vm://0")
    configuration.addAcceptorConfiguration("invm-acceptor", "vm://0")

    def jmsConfiguration = new JMSConfigurationImpl()
    def cfConfig = new ConnectionFactoryConfigurationImpl()
        .setName("InVmConnectionFactory")
        .setConnectorNames("invm-connector")
        .setBindings("cf")
    jmsConfiguration.setConnectionFactoryConfigurations([cfConfig])

    broker = new EmbeddedJMS()
    broker.setConfiguration(configuration)
    broker.setJmsConfiguration(jmsConfiguration)
    broker.start()

    // Minimal Spring context: @EnableJms registers the JmsListenerAnnotationBeanPostProcessor
    // which wires the @JmsListener on the production listener to the broker.
    context = new AnnotationConfigApplicationContext(
        TestJmsConfiguration,
        AclCacheEvictionListener,
        AclEvictionMessagePublisher)

    awaitTopicSubscription()
  }

  /**
   * Waits until the broker actually has a consumer on the eviction topic.
   *
   * <p>The topic is <b>non-durable</b> pub-sub, so the broker discards a message published before
   * the subscription exists. That subscription is created asynchronously after the listener
   * container starts consuming, which means waiting for
   * {@code JmsListenerEndpointRegistry#isRunning()} is not enough: it reports that the
   * <em>container</em> started, not that the <em>subscription</em> reached the broker. Under CI
   * load the gap between the two is wide enough that the published eviction was dropped and the
   * test then waited out its receipt budget for a message that would never arrive — the failure
   * seen in GitHub Actions, while the same test passed locally.</p>
   *
   * <p>So this asserts the state the test actually needs: a bound queue on the topic address with
   * at least one consumer. Keep this in preference to a container check; simplifying it back
   * reintroduces the race.</p>
   */
  private void awaitTopicSubscription() {
    // First wait for the listener container itself, so a genuinely broken wiring still fails with
    // a clear message rather than timing out on the subscription check below.
    def registry = context.getBean(JmsListenerEndpointRegistry)
    def containerDeadline = System.nanoTime() + Duration.ofSeconds(10).toNanos()
    while (!registry.listenerContainers.findAll { it.isRunning() } &&
            System.nanoTime() < containerDeadline) {
      Thread.sleep(50)
    }
    assert registry.listenerContainers.findAll { it.isRunning() }:
        "JMS listener container did not become ready in time"

    def postOffice = broker.getJMSServerManager().getActiveMQServer().postOffice
    def address = SimpleString.of(TOPIC)
    def deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos()
    while (System.nanoTime() < deadline) {
      def subscribed = subscribedConsumers(postOffice, address) > 0
      if (subscribed) {
        return
      }
      Thread.sleep(50)
    }
    assert false: "no consumer subscribed to topic '%s' within the readiness budget".formatted(TOPIC)
  }

  /**
   * The number of consumers across every binding of the given address.
   *
   * <p>A {@code Binding} is not itself a {@code Queue}: a {@code LocalQueueBinding} wraps one and
   * exposes it through {@code getBindable()}. Testing {@code binding instanceof Queue} always
   * returned zero, which made the readiness check fail even when the subscription was present.</p>
   */
  private static int subscribedConsumers(postOffice, SimpleString address) {
    def bindings = postOffice.getBindingsForAddress(address)
    if (bindings == null) {
      return 0
    }
    int total = 0
    bindings.getBindings().each { binding ->
      def bindable = binding.bindable
      if (bindable instanceof Queue) {
        total += ((Queue) bindable).consumerCount
      }
    }
    return total
  }

  def cleanup() {
    if (context != null) {
      context.close()
    }
    if (broker != null) {
      broker.stop()
    }
  }

  def "publishing an ACL eviction evicts the cache entry on the receiving side within the NFR"() {
    given: "a project whose ACL changed on the writing instance"
    ProjectId projectId = ProjectId.parse("9f7a6e3e-2d1e-4a6b-8c5d-9a0b1c2d3e4f")
    CountingAclCache aclCache = context.getBean(CountingAclCache)
    AclEvictionPublisher publisher = context.getBean(AclEvictionPublisher)

    when: "the eviction is broadcast"
    long startNanos = System.nanoTime()
    publisher.publishAclEviction(projectId)

    then: "the listener evicts the project's ObjectIdentity well within the 60s budget"
    aclCache.awaitEviction(1, Duration.ofSeconds(5))
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
    assert elapsedMillis < 60_000
    aclCache.evictedIdentities.size() == 1
    aclCache.evictedIdentities.first() == new ObjectIdentityImpl(Project.class, projectId)
  }

  def "evicts each affected project exactly once per broadcast"() {
    given: "two projects whose ACL changed"
    ProjectId first = ProjectId.parse("9f7a6e3e-2d1e-4a6b-8c5d-9a0b1c2d3e4f")
    ProjectId second = ProjectId.parse("aa7a6e3e-2d1e-4a6b-8c5d-9a0b1c2d3e4f")
    CountingAclCache aclCache = context.getBean(CountingAclCache)
    AclEvictionPublisher publisher = context.getBean(AclEvictionPublisher)

    when: "eviction events are broadcast for both projects"
    publisher.publishAclEviction(first)
    publisher.publishAclEviction(second)

    then: "both project entries are evicted exactly once"
    aclCache.awaitEviction(2, Duration.ofSeconds(10))
    aclCache.evictedIdentities.size() == 2
    aclCache.evictedIdentities.containsAll([
        new ObjectIdentityImpl(Project.class, first),
        new ObjectIdentityImpl(Project.class, second)])
  }

  private static String createTempDirectory(String prefix) {
    def dir = File.createTempDir(prefix)
    dir.absolutePath
  }

  /** In-memory {@link AclCache} recording every evicted {@link ObjectIdentity}. */
  static class CountingAclCache implements AclCache {

    final ConcurrentLinkedQueue<ObjectIdentity> evictedIdentities = new ConcurrentLinkedQueue<>()

    @Override
    void evictFromCache(ObjectIdentity objectIdentity) {
      evictedIdentities.add(objectIdentity)
    }

    @Override
    void evictFromCache(Serializable serializable) {
      // not used by the listener path
    }

    @Override
    MutableAcl getFromCache(ObjectIdentity objectIdentity) { return null }

    @Override
    MutableAcl getFromCache(Serializable serializable) { return null }

    @Override
    void putInCache(MutableAcl acl) { }

    @Override
    void clearCache() { }

    /** Blocks until {@code count} evictions have been observed or the timeout elapses. */
    boolean awaitEviction(int count, Duration timeout) {
      def deadline = System.nanoTime() + timeout.toNanos()
      while (evictedIdentities.size() < count && System.nanoTime() < deadline) {
        Thread.sleep(50)
      }
      return evictedIdentities.size() >= count
    }
  }

  @Configuration
  @EnableJms
  static class TestJmsConfiguration {

    @Bean
    static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
      def configurer = new PropertySourcesPlaceholderConfigurer()
      def properties = new Properties()
      properties.setProperty("qbic.broadcasting.acl-eviction.topic", TOPIC)
      configurer.setProperties(properties)
      return configurer
    }

    @Bean
    jakarta.jms.ConnectionFactory connectionFactory() {
      return new ActiveMQConnectionFactory("vm://0")
    }

    @Bean
    JmsTemplate jmsTemplate(jakarta.jms.ConnectionFactory connectionFactory) {
      JmsTemplate template = new JmsTemplate(connectionFactory)
      template.setPubSubDomain(true)
      return template
    }

    @Bean
    JmsListenerContainerFactory jmsListenerContainerFactory(
        jakarta.jms.ConnectionFactory connectionFactory) {
      def factory = new DefaultJmsListenerContainerFactory()
      factory.setConnectionFactory(connectionFactory)
      factory.setPubSubDomain(true)
      factory.setMessageConverter(new SimpleMessageConverter())
      return factory
    }

    @Bean
    AclCache aclCache() {
      return new CountingAclCache()
    }

    @Bean
    CacheManager cacheManager() {
      return new org.springframework.cache.concurrent.ConcurrentMapCacheManager("acl_cache")
    }
  }
}