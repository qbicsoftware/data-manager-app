package life.qbic.datamanager.security;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * <b>ACL cache eviction listener</b>
 *
 * <p>Consumes {@code aclCacheEvicted} integration events broadcast on the dedicated eviction
 * topic and evicts the affected project's ACL from the process-local {@code acl_cache}.</p>
 *
 * <p>The {@code acl_cache} is per-instance EHCache with a 600s TTL (see {@code ehcache3.xml});
 * a grant mutation only evicts the cache on the writing instance. This listener makes the
 * revocation/role-change effective on <b>every</b> instance within one broker hop, satisfying the
 * ≤60s revocation NFR (strategy §4.6, plan D4).</p>
 *
 * <p>Follows the {@code MessageConsumer} tolerance contract: malformed input or unknown event
 * types are logged and ignored — an exception must never leave the listener thread because it
 * would terminate consumption for subsequent messages.</p>
 *
 * @since 1.0.0
 */
@Component
public class AclCacheEvictionListener {

  private static final Logger log = logger(AclCacheEvictionListener.class);

  /**
   * Shared mapper for the JSON deserialization of {@link IntegrationEvent}s.
   *
   * <p>tools.jackson {@link ObjectMapper}s are thread-safe after configuration and can be reused
   * across concurrent listener invocations without per-call construction overhead.</p>
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static final String ACL_EVICTION_EVENT_TYPE = "aclCacheEvicted";
  static final String PROJECT_ID_KEY = "projectId";

  private final AclCache aclCache;
  private final CacheManager cacheManager;

  @Autowired
  public AclCacheEvictionListener(AclCache aclCache, CacheManager cacheManager) {
    this.aclCache = aclCache;
    this.cacheManager = cacheManager;
  }

  @JmsListener(destination = "${qbic.broadcasting.acl-eviction.topic}")
  public void onAclEvictionEvent(String content) {
    log.debug("Incoming ACL eviction message: %s".formatted(content));
    IntegrationEvent event = parse(content);
    if (event == null || !ACL_EVICTION_EVENT_TYPE.equals(event.type())) {
      return;
    }
    String projectIdValue = event.content().get(PROJECT_ID_KEY);
    if (projectIdValue == null || projectIdValue.isBlank() || !ProjectId.isValid(projectIdValue)) {
      log.error("ACL eviction event for project %s ignored: missing or invalid projectId".formatted(
          projectIdValue));
      return;
    }
    ProjectId projectId = ProjectId.parse(projectIdValue);
    // Robust eviction: the raw Spring cache holds the ACL under both the ObjectIdentity and the
    // AclImpl id keys; SpringCacheBasedAclCache.evictFromCache only evicts if it finds the entry
    // first. Clearing the whole process-local "acl_cache" guarantees the revoked grant cannot
    // survive in this instance (≤60s revocation NFR).
    org.springframework.cache.Cache cache = cacheManager.getCache("acl_cache");
    if (cache != null) {
      cache.evict(new ObjectIdentityImpl(Project.class, projectId));
      cache.clear();
    }
    aclCache.evictFromCache(new ObjectIdentityImpl(Project.class, projectId));
    log.debug("Evicted ACL cache entry for project %s".formatted(projectId.value()));
  }

  private IntegrationEvent parse(String content) {
    try {
      return OBJECT_MAPPER.readValue(content, IntegrationEvent.class);
    } catch (JacksonException e) {
      log.error("Json to object mapping failed!", e);
      return null;
    }
  }
}