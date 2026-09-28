package life.qbic.datamanager.security

import life.qbic.projectmanagement.domain.model.project.Project
import life.qbic.projectmanagement.domain.model.project.ProjectId
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.model.AclCache
import spock.lang.Specification

/**
 * Unit tests for the {@link AclCacheEvictionListener}.
 *
 * <p>Verifies that {@code aclCacheEvicted} integration events evict the affected project's ACL
 * entry from the process-local {@code acl_cache} and that unknown or malformed messages are
 * tolerated (MessageConsumer contract — an exception must never leave the listener thread).</p>
 */
class AclCacheEvictionListenerSpec extends Specification {

  static final String PROJECT_UUID = "9f7a6e3e-2d1e-4a6b-8c5d-9a0b1c2d3e4f"

  AclCache aclCache = Mock()
  AclCacheEvictionListener listener

  def setup() {
    listener = new AclCacheEvictionListener(aclCache)
  }

  def "evicts the ACL cache entry for the project referenced in a valid event"() {
    given: "a project whose ACL was changed on another instance"
    ProjectId projectId = ProjectId.parse(PROJECT_UUID)
    String content = evictionEvent(PROJECT_UUID)

    when: "the eviction event is received"
    listener.onAclEvictionEvent(content)

    then: "the local cache entry for that project is evicted exactly once"
    1 * aclCache.evictFromCache(new ObjectIdentityImpl(Project.class, projectId))
  }

  def "ignores events of an unknown type"() {
    given: "an event that is not an ACL eviction"
    String content = '{"type":"someOtherEvent","content":{"projectId":"' + PROJECT_UUID + '"}}'

    when:
    listener.onAclEvictionEvent(content)

    then: "no eviction happens"
    0 * aclCache.evictFromCache(*_)
  }

  def "tolerates malformed JSON without throwing and without evicting"() {
    given: "a payload that is not valid JSON"
    String content = "this is not json {"

    when:
    listener.onAclEvictionEvent(content)

    then: "the listener degrades gracefully"
    noExceptionThrown()
    0 * aclCache.evictFromCache(*_)
  }

  def "ignores events without a project id"() {
    given: "an eviction event missing the project id"
    String content = '{"type":"aclCacheEvicted","content":{}}'

    when:
    listener.onAclEvictionEvent(content)

    then: "no eviction happens"
    0 * aclCache.evictFromCache(*_)
  }

  def "ignores eviction events with an invalid project id"() {
    given: "an eviction event whose project id is not a uuid"
    String content = '{"type":"aclCacheEvicted","content":{"projectId":"not-a-uuid"}}'

    when:
    listener.onAclEvictionEvent(content)

    then: "no eviction happens and no exception escapes the listener"
    noExceptionThrown()
    0 * aclCache.evictFromCache(*_)
  }

  private static String evictionEvent(String projectId) {
    return '{"type":"aclCacheEvicted","content":{"projectId":"' + projectId + '"}}'
  }
}