package life.qbic.projectmanagement.application.authorization.acl;

import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>ACL Eviction Publisher</b>
 *
 * <p>Notifies all running application instances that the access control list of a project has
 * changed and must be re-fetched from the database.</p>
 *
 * <p>Each instance maintains a process-local {@code acl_cache} (EHCache, TTL 600s). A grant
 * mutation performed on one instance evicts only the local cache entry; without cross-instance
 * propagation, other instances would honour stale entries for up to the cache TTL. Publishing an
 * eviction signal for the affected project enables every instance to drop its cached entry
 * immediately, keeping revocation effective within the ≤60s NFR (strategy §4.6, plan D4).</p>
 *
 * <p>This is an application-layer port: implementations belong to the infrastructure layer and
 * must not leak messaging concerns (JMS, broker) through this interface.</p>
 *
 * @since 1.0.0
 */
public interface AclEvictionPublisher {

  /**
   * Publishes an eviction signal for the ACL cache entry of the given project.
   *
   * @param projectId the project whose ACL cache entry shall be evicted on all instances
   */
  void publishAclEviction(ProjectId projectId);
}