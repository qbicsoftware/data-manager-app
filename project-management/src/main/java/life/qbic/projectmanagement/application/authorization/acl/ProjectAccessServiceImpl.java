package life.qbic.projectmanagement.application.authorization.acl;

import static java.util.function.Predicate.not;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.service.event.ProjectAccessGranted;
import life.qbic.usergroups.api.GroupInformationService;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.cache.CacheManager;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAccessServiceImpl implements ProjectAccessService {

  private static final Logger log = logger(ProjectAccessServiceImpl.class);
  public static final String SELECT_IDENTITY = "SELECT @@IDENTITY";
  public static final String GROUP_SID_PREFIX = "GROUP_";
  private final MutableAclService aclService;
  private final JdbcTemplate jdbcTemplate;
  private final GroupInformationService groupInformationService;
  private @org.springframework.context.annotation.Lazy AclCache aclCache;
  private AclEvictionPublisher aclEvictionPublisher;
  private CacheManager cacheManager;

  public ProjectAccessServiceImpl(@Autowired MutableAclService aclService,
      JdbcTemplate jdbcTemplate, @Autowired GroupInformationService groupInformationService) {
    this.aclService = aclService;
    this.jdbcTemplate = jdbcTemplate;
    this.groupInformationService = groupInformationService;
  }

  @Autowired(required = false)
  void setAclCache(@org.springframework.context.annotation.Lazy AclCache aclCache) {
    this.aclCache = aclCache;
  }

  @Autowired(required = false)
  void setAclEvictionPublisher(AclEvictionPublisher aclEvictionPublisher) {
    this.aclEvictionPublisher = aclEvictionPublisher;
  }

  @Autowired(required = false)
  void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  private static MutableAcl getAclForProject(ProjectId projectId, List<Sid> sids,
      MutableAclService mutableAclService) {
    ObjectIdentityImpl objectIdentity = new ObjectIdentityImpl(Project.class, projectId);
    MutableAcl acl;
    JdbcMutableAclService serviceImpl = (JdbcMutableAclService) mutableAclService;
    // these settings are necessary for MySQL to correctly throw several types of exceptions
    // instead of an unrelated exception related to the identity function
    serviceImpl.setClassIdentityQuery(SELECT_IDENTITY);
    serviceImpl.setSidIdentityQuery(SELECT_IDENTITY);
    try {
      acl = (MutableAcl) serviceImpl.readAclById(objectIdentity, sids);
    } catch (NotFoundException e) {
      acl = createAclForProject(projectId, mutableAclService);
    }
    return acl;
  }

  private static MutableAcl createAclForProject(ProjectId projectId,
      MutableAclService mutableAclService) {
    ObjectIdentityImpl objectIdentity = new ObjectIdentityImpl(Project.class, projectId);
    JdbcMutableAclService serviceImpl = (JdbcMutableAclService) mutableAclService;
    // these settings are necessary for MySQL to correctly throw several types of exceptions
    // instead of an unrelated exception related to the identity function
    serviceImpl.setClassIdentityQuery(SELECT_IDENTITY);
    serviceImpl.setSidIdentityQuery(SELECT_IDENTITY);
    return serviceImpl.createAcl(objectIdentity);
  }

  private static Set<Permission> parsePermissions(
      Entry<Sid, List<AccessControlEntry>> sidListEntry) {
    return sidListEntry.getValue().stream()
        .map(AccessControlEntry::getPermission)
        .collect(Collectors.toSet());
  }

  /**
   * Rejects granting the project OWNER role to a non-principal (authority) sid.
   *
   * <p>User groups ride as {@link GrantedAuthoritySid}s (reserved {@code GROUP_} prefix, strategy
   * §4.3). A group must never become the ACL owner — ownership is a personal, principal-sid
   * property. The guard is authority-agnostic on purpose: since groups are type-identical to
   * {@code ROLE_*} authorities at the ACL layer, no non-principal sid may ever become the owner.
   * Enforcement happens at the service boundary because the ACL write path is reachable outside
   * the UI (e.g. the project repository seeding ADMIN grants for system roles).</p>
   *
   * @param projectId the project whose ACL is about to be changed
   * @param authority the authority string whose ownership grant is refused
   * @param projectRole the requested project role
   * @throws ApplicationException if the requested role is {@link ProjectRole#OWNER}
   */
  private void rejectAuthorityOwnership(ProjectId projectId, String authority,
      ProjectRole projectRole) {
    if (ProjectRole.OWNER.equals(projectRole)) {
      throw new ApplicationException(
          "Authority %s can never become the project OWNER of project %s. Groups and roles are granted READ, WRITE or ADMIN only."
              .formatted(authority, projectId));
    }
  }

  /**
   * Evicts the cached ACL for the given project from the process-local acl cache.
   *
   * <p>Spring's {@code JdbcMutableAclService.updateAcl} evicts the {@code acl_cache} entry only on
   * the executing node. This is sufficient for single-node correctness (the revoked/updated grant
   * is effective at the next permission check on this node). Cross-instance propagation of the
   * revocation NFR (≤60s) is addressed by the broadcast-eviction mechanism tracked as a follow-up
   * (strategy §4.6, plan D5).</p>
   *
   * @param projectId the project whose ACL cache entry shall be evicted
   */
  private void evictCachedAcl(ProjectId projectId) {
    // SpringCacheBasedAclCache.evictFromCache only evicts when the entry is currently present
    // under the ObjectIdentity key (it looks the entry up first). To make revocation of a
    // group/role grant effective at the next authorization check even if the entry is keyed
    // differently (the cache stores both under the ObjectIdentity AND the AclImpl id), evict
    // the raw Spring cache directly. Clearing the whole "acl_cache" is acceptable: project
    // sharIng writes are rare, and it guarantees no stale ACL survives on this instance.
    // Cross-instance propagation of the ≤60s revocation NFR is handled by the broadcast
    // eviction (each node evicts its own process-local cache the same way).
    try {
      if (cacheManager != null) {
        org.springframework.cache.Cache cache = cacheManager.getCache("acl_cache");
        if (cache != null) {
          cache.evict(new ObjectIdentityImpl(Project.class, projectId));
          cache.clear();
          log.debug("[DIAG] evictCachedAcl: raw cache cleared for project %s".formatted(
              projectId.value()));
        }
      }
    } catch (RuntimeException e) {
      log.warn("Could not clear acl_cache via CacheManager for project %s: %s".formatted(
          projectId.value(), e.getMessage()));
    }
    if (aclCache != null) {
      aclCache.evictFromCache(new ObjectIdentityImpl(Project.class, projectId));
    }
  }

  private void fireProjectAccessGranted(String userId, ProjectId projectId) {
    var projectAccessGranted = ProjectAccessGranted.create(userId, projectId.value());
    DomainEventDispatcher.instance().dispatch(projectAccessGranted);
  }

  /**
   * Publishes an eviction signal for the given project's ACL cache entry to all running
   * instances.
   *
   * <p>The local {@link AclCache} is evicted synchronously by {@link #evictCachedAcl(ProjectId)};
   * this broadcast makes the change effective on the other instances (≤60s revocation NFR, plan
   * D4). The publisher is optional — if no implementation is on the classpath (e.g. isolated
   * unit tests), the call degrades to a no-op.</p>
   *
   * @param projectId the project whose ACL cache entries shall be evicted everywhere
   */
  private void publishAclEviction(ProjectId projectId) {
    if (aclEvictionPublisher != null) {
      aclEvictionPublisher.publishAclEviction(projectId);
    }
  }

  @Override
  @Transactional
  public void initializeProject(ProjectId projectId, String userId) {
    try {
      MutableAcl aclForProject = createAclForProject(projectId, aclService);
      PrincipalSid principalSid = new PrincipalSid(userId);
      aclForProject.setOwner(principalSid);
      var permissions = ProjectRole.OWNER.toPermissions();
      for (Permission permission : permissions) {
        aclForProject.insertAce(aclForProject.getEntries().size(), permission, principalSid, true);
      }
      aclService.updateAcl(aclForProject);
      log.debug("Initialized project %s with owner %s".formatted(projectId.value(), userId));
    } catch (AlreadyExistsException e) {
      throw new ApplicationException("User %s tried to create ACL for project %s"
          .formatted(SecurityContextHolder.getContext().getAuthentication().getName(),
              projectId.value())
          , e);
    }
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void addCollaborator(ProjectId projectId, String userId, ProjectRole projectRole) {
    PrincipalSid principalSid = new PrincipalSid(userId);
    MutableAcl aclForProject = getAclForProject(projectId, List.of(principalSid), aclService);

    if (ProjectRole.OWNER.equals(projectRole)) {
      log.debug("Project %s owner changed from %s to user %s".formatted(projectId.value(),
          aclForProject.getOwner(), projectId));
      aclForProject.setOwner(principalSid);
      aclService.updateAcl(aclForProject);
//      fireProjectAccessGranted(userId, projectId);
//      return;
    }

    Collection<Permission> permissions = projectRole.toPermissions();
    boolean userHasAccess = aclForProject.getEntries().stream()
        .filter(accessControlEntry -> accessControlEntry.getSid() instanceof PrincipalSid)
        .anyMatch(accessControlEntry -> accessControlEntry.getSid().equals(principalSid));
    if (userHasAccess) {
      /* This is important!
       * Consider adding a person as admin and then adding them again as reader.
       * This leads to redundant access control entries.
       */
      throw new ApplicationException(
          "User %s already collaborates on %s. Please change the project role instead".formatted(
              userId, projectId));
    }
    for (Permission permission : permissions) {
      aclForProject.insertAce(aclForProject.getEntries().size(), permission, principalSid, true);
    }
    log.debug("User %s now collaborates on project %s as %s.".formatted(userId, projectId.value(),
        projectRole.label()));
    fireProjectAccessGranted(userId, projectId);
    aclService.updateAcl(aclForProject);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void removeCollaborator(ProjectId projectId, String userId) {
    PrincipalSid principalSid = new PrincipalSid(userId);
    MutableAcl aclForProject = getAclForProject(projectId, List.of(principalSid), aclService);
    List<AccessControlEntry> entries = aclForProject.getEntries();
    if (Objects.isNull(entries)) {
      log.warn("No ACEs found for project " + projectId);
      return;
    }
    for (int i = entries.size() - 1; i >= 0; i--) {
      AccessControlEntry accessControlEntry = entries.get(i);
      if (principalSid.equals(accessControlEntry.getSid())) {
        aclForProject.deleteAce(i);
      }
    }
    aclService.updateAcl(aclForProject);
    log.debug("User %s no longer collaborates on project %s.".formatted(userId, projectId.value()));
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void changeRole(ProjectId projectId, String userId, ProjectRole projectRole) {
    PrincipalSid principalSid = new PrincipalSid(userId);
    MutableAcl aclForProject = getAclForProject(projectId, List.of(principalSid), aclService);

    Collection<Permission> requiredPermissions = projectRole.toPermissions();

    if (ProjectRole.OWNER.equals(projectRole)) {
      Sid previousOwner = aclForProject.getOwner();
      log.debug("Project %s owner changed from %s to user %s".formatted(projectId.value(),
          previousOwner, projectId));
      aclForProject.setOwner(principalSid);
      requiredPermissions = List.of();
    }

    Set<Permission> currentPermissions = aclForProject.getEntries().stream()
        .filter(accessControlEntry -> accessControlEntry.getSid().equals(principalSid))
        .map(AccessControlEntry::getPermission)
        .collect(Collectors.toUnmodifiableSet());

    Set<Permission> additionalPermissions = requiredPermissions.stream()
        .filter(not(currentPermissions::contains))
        .collect(Collectors.toUnmodifiableSet());

    Set<Permission> noLongerValidPermissions = currentPermissions.stream()
        .filter(not(requiredPermissions::contains))
        .collect(Collectors.toUnmodifiableSet());

    List<AccessControlEntry> entries = aclForProject.getEntries();
    for (int entryIndex = entries.size() - 1; entryIndex >= 0; entryIndex--) {
      AccessControlEntry entry = entries.get(entryIndex);
      if (!entry.getSid().equals(principalSid)) {
        continue;
      }
      if (noLongerValidPermissions.contains(entry.getPermission()) && entry.isGranting()) {
        aclForProject.deleteAce(entryIndex);
      }
    }

    for (Permission additionalPermission : additionalPermissions) {
      aclForProject.insertAce(aclForProject.getEntries().size(), additionalPermission,
          principalSid, true);
    }

    aclService.updateAcl(aclForProject);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void addAuthorityAccess(ProjectId projectId, String authority, ProjectRole projectRole) {
    GrantedAuthoritySid authoritySid = new GrantedAuthoritySid(authority);
    rejectAuthorityOwnership(projectId, authority, projectRole);
    // read the full ACL (not sid-filtered): sid-filtered reads expose only the requested sid's
    // entries, which breaks the ACL authorization check inside insertAce/deleteAce when the
    // acting group-admin member is not the authority being modified. An empty sid list means
    // "no filtering" to Spring's readAclById (identical semantics to null, but null-conflicts
    // with the two-arg overload resolution in some callers).
    MutableAcl aclForProject = getAclForProject(projectId, List.of(), aclService);

    Collection<Permission> permissions = projectRole.toPermissions();
    boolean authorityHasAccess = aclForProject.getEntries().stream()
        .filter(accessControlEntry -> accessControlEntry.getSid() instanceof GrantedAuthoritySid)
        .anyMatch(
            accessControlEntry -> ((GrantedAuthoritySid) accessControlEntry.getSid()).getGrantedAuthority()
                .equals(authority));
    if (authorityHasAccess) {
      // Self-heal: re-granting an authority that already has access is treated as a role change.
      // This corrects stale or partial ACEs (for example left behind by an old removal) instead
      // of blocking the user with "already collaborates" while the grant is invisible/incorrect.
      doChangeAuthorityAccess(projectId, authority, projectRole);
      return;
    }
    for (Permission permission : permissions) {
      aclForProject.insertAce(aclForProject.getEntries().size(), permission, authoritySid, true);
    }
    log.debug(
        "Authority %s now collaborates on project %s as %s.".formatted(authority, projectId.value(),
            projectRole.label()));
    aclService.updateAcl(aclForProject);
    evictCachedAcl(projectId);
    publishAclEviction(projectId);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void removeAuthorityAccess(ProjectId projectId, String authority) {
    GrantedAuthoritySid grantedAuthoritySid = new GrantedAuthoritySid(authority);
    MutableAcl aclForProject = getAclForProject(projectId, List.of(), aclService);
    List<AccessControlEntry> entries = aclForProject.getEntries();
    // Iterate backwards: deleteAce removes from the live entries list, so a forward loop would
    // skip every other entry and leave partial ACEs behind (which then map to no role and block
    // a re-grant while being invisible in the roster). Mirrors removeCollaborator/changeAuthority.
    for (int entryIndex = entries.size() - 1; entryIndex >= 0; entryIndex--) {
      AccessControlEntry accessControlEntry = entries.get(entryIndex);
      if (accessControlEntry.getSid().equals(grantedAuthoritySid)) {
        aclForProject.deleteAce(entryIndex);
      }
    }
    log.debug("Authority %s no longer collaborates on project %s.".formatted(authority,
        projectId.value()));
    aclService.updateAcl(aclForProject);
    evictCachedAcl(projectId);
    publishAclEviction(projectId);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void changeAuthorityAccess(ProjectId projectId, String authority,
      ProjectRole projectRole) {
    log.debug("[DIAG] changeAuthorityAccess start project=%s authority=%s newRole=%s".formatted(
        projectId.value(), authority, projectRole));
    try {
      doChangeAuthorityAccess(projectId, authority, projectRole);
      log.debug("[DIAG] changeAuthorityAccess ok project=%s authority=%s newRole=%s".formatted(
          projectId.value(), authority, projectRole));
    } catch (RuntimeException e) {
      log.error("[DIAG] changeAuthorityAccess FAILED project=%s authority=%s newRole=%s".formatted(
          projectId.value(), authority, projectRole), e);
      throw e;
    }
  }

  private void doChangeAuthorityAccess(ProjectId projectId, String authority,
      ProjectRole projectRole) {
    GrantedAuthoritySid authoritySid = new GrantedAuthoritySid(authority);
    rejectAuthorityOwnership(projectId, authority, projectRole);
    // read the full ACL (not sid-filtered) so the acting group-admin member's own admin ACE is
    // visible to the ACL authorization check performed inside deleteAce/insertAce. Empty sid
    // list = no filtering (see addAuthorityAccess for the rationale).
    MutableAcl aclForProject = getAclForProject(projectId, List.of(), aclService);

    Collection<Permission> requiredPermissions = projectRole.toPermissions();

    Set<Permission> currentPermissions = aclForProject.getEntries().stream()
        .filter(accessControlEntry -> accessControlEntry.getSid().equals(authoritySid))
        .map(AccessControlEntry::getPermission)
        .collect(Collectors.toUnmodifiableSet());

    Set<Permission> additionalPermissions = requiredPermissions.stream()
        .filter(not(currentPermissions::contains))
        .collect(Collectors.toUnmodifiableSet());

    Set<Permission> noLongerValidPermissions = currentPermissions.stream()
        .filter(not(requiredPermissions::contains))
        .collect(Collectors.toUnmodifiableSet());

    List<AccessControlEntry> entries = aclForProject.getEntries();
    for (int entryIndex = entries.size() - 1; entryIndex >= 0; entryIndex--) {
      AccessControlEntry entry = entries.get(entryIndex);
      if (!entry.getSid().equals(authoritySid)) {
        continue;
      }
      if (noLongerValidPermissions.contains(entry.getPermission()) && entry.isGranting()) {
        aclForProject.deleteAce(entryIndex);
      }
    }

    for (Permission additionalPermission : additionalPermissions) {
      aclForProject.insertAce(aclForProject.getEntries().size(), additionalPermission,
          authoritySid, true);
    }

    aclService.updateAcl(aclForProject);
    evictCachedAcl(projectId);
    publishAclEviction(projectId);
  }

  @Override
  public List<ProjectId> getAccessibleProjectsForSid(String sid) {
    Object[] args = {sid, Project.class.getName()};
    var accessibleProjectsForSid = jdbcTemplate.query(getProjectsWithAccessQuery(), getRowMapper(),
        args);
    var accessibleProjectIds = new ArrayList<ProjectId>();
    if (!accessibleProjectsForSid.isEmpty()) {
      accessibleProjectIds.addAll(accessibleProjectsForSid.stream().distinct()
          .map(objectIdentity -> objectIdentity.getIdentifier().toString())
          .map(ProjectId::parse).toList());
    }
    return accessibleProjectIds;
  }

  /*Taken and adapted from
 https://stackoverflow.com/questions/30133667/how-to-get-a-list-of-objects-that-a-user-can-access-using-acls-related-tables#40275173*/
  private static String getProjectsWithAccessQuery() {
    return "SELECT " +
        "    obj.object_id_identity AS obj_id, " +
        "    class.class AS class " +
        "FROM " +
        "    acl_object_identity obj, " +
        "    acl_class class, " +
        "    acl_entry entry " +
        "WHERE " +
        "    obj.object_id_class = class.id " +
        "    and entry.granting = true " +
        "    and entry.acl_object_identity = obj.id " +
        "    and entry.sid = (SELECT id FROM acl_sid WHERE sid = ?) " +
        "    and obj.object_id_class = (SELECT id FROM acl_class WHERE acl_class.class = ?) " +
        "GROUP BY " +
        "    obj.object_id_identity, " +
        "    class.class ";
  }

  private RowMapper<ObjectIdentity> getRowMapper() {
    return (rs, rowNum) -> {
      String javaType = rs.getString("class");
      String identifier = rs.getString("obj_id");
      return new ObjectIdentityImpl(javaType, identifier);
    };
  }

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public List<ProjectCollaborator> listCollaborators(ProjectId projectId) {
    Acl acl = aclService.readAclById(new ObjectIdentityImpl(Project.class, projectId), null);

    var collaborators = new ArrayList<ProjectCollaborator>();
    if (acl.getOwner() instanceof PrincipalSid principalSid) {
      ProjectCollaborator owner = new ProjectCollaborator(principalSid.getPrincipal(),
          projectId,
          ProjectRole.OWNER);
      collaborators.add(owner);
    }
    Map<Sid, List<AccessControlEntry>> entriesBySid = acl.getEntries().stream()
        .collect(Collectors.groupingBy(AccessControlEntry::getSid));

    Set<ProjectCollaborator> otherCollaborators = entriesBySid.entrySet().stream()
        // skip the owner as it is handled explicitly
        .filter(sidListEntry -> !acl.getOwner().equals(sidListEntry.getKey()))
        .filter(sidListEntry -> sidListEntry.getKey() instanceof PrincipalSid)
        .filter(sidListEntry -> {
          //only show resolvable project roles
          Set<Permission> permissions = parsePermissions(sidListEntry);
          return ProjectRole.fromPermissions(permissions).isPresent();
        })
        .map(sidListEntry -> {
          Set<Permission> permissions = parsePermissions(sidListEntry);
          Optional<ProjectRole> roleFromPermissions = ProjectRole.fromPermissions(permissions);
          ProjectRole projectRole = roleFromPermissions.orElseThrow();
          return new ProjectCollaborator(((PrincipalSid) sidListEntry.getKey()).getPrincipal(),
              projectId, projectRole);
        })
        .collect(Collectors.toUnmodifiableSet());
    collaborators.addAll(otherCollaborators);
    return collaborators;
  }

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public List<SharedProjectGroup> listSharedGroups(ProjectId projectId) {
    Acl acl = aclService.readAclById(new ObjectIdentityImpl(Project.class, projectId), null);

    Map<String, List<AccessControlEntry>> entriesByGroupSid = acl.getEntries().stream()
        .filter(accessControlEntry -> accessControlEntry.getSid() instanceof GrantedAuthoritySid)
        .filter(accessControlEntry -> ((GrantedAuthoritySid) accessControlEntry.getSid())
            .getGrantedAuthority().startsWith(GROUP_SID_PREFIX))
        .collect(Collectors.groupingBy(accessControlEntry ->
            ((GrantedAuthoritySid) accessControlEntry.getSid()).getGrantedAuthority()));

    return entriesByGroupSid.entrySet().stream()
        .map(groupSidEntry -> {
          String groupSid = groupSidEntry.getKey();
          String groupId = groupSid.substring(GROUP_SID_PREFIX.length());
          Set<Permission> permissions = groupSidEntry.getValue().stream()
              .map(AccessControlEntry::getPermission)
              .collect(Collectors.toSet());
          var groupInfo = groupInformationService.findGroupById(groupId);
          if (groupInfo.isEmpty()) {
            return null;
          }
          ProjectRole role = ProjectRole.fromPermissions(permissions)
              .orElseGet(() -> fallbackRole(permissions));
          return new SharedProjectGroup(groupId, groupInfo.get().name(),
              groupInfo.get().description(), projectId, role);
        })
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Maps a corrupt/partial ACE (for example one left over from a failed removal) to the closest
   * role so the grant stays visible in the roster and can be corrected or revoked, instead of
   * silently disappearing while still blocking a re-grant.
   */
  private static ProjectRole fallbackRole(Set<Permission> permissions) {
    if (permissions.contains(BasePermission.ADMINISTRATION)) {
      return ProjectRole.ADMIN;
    }
    if (permissions.contains(BasePermission.WRITE)) {
      return ProjectRole.WRITE;
    }
    return ProjectRole.READ;
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void removeProject(ProjectId projectId) {
    ObjectIdentityImpl objectIdentity = new ObjectIdentityImpl(Project.class, projectId);
    aclService.deleteAcl(objectIdentity, true);
  }


}
