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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.AccessControlEntry;
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
  private final MutableAclService aclService;
  private final JdbcTemplate jdbcTemplate;

  public ProjectAccessServiceImpl(@Autowired MutableAclService aclService,
      JdbcTemplate jdbcTemplate) {
    this.aclService = aclService;
    this.jdbcTemplate = jdbcTemplate;
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

  private void fireProjectAccessGranted(String userId, ProjectId projectId) {
    var projectAccessGranted = ProjectAccessGranted.create(userId, projectId.value());
    DomainEventDispatcher.instance().dispatch(projectAccessGranted);
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
    MutableAcl aclForProject = getAclForProject(projectId, List.of(authoritySid), aclService);

    if (ProjectRole.OWNER.equals(projectRole)) {
      log.debug("Project %s owner changed from %s to authority %s".formatted(projectId.value(),
          aclForProject.getOwner(), projectId));
      aclForProject.setOwner(authoritySid);
      aclService.updateAcl(aclForProject);
      return;
    }

    Collection<Permission> permissions = projectRole.toPermissions();
    boolean authorityHasAccess = aclForProject.getEntries().stream()
        .filter(accessControlEntry -> accessControlEntry.getSid() instanceof GrantedAuthoritySid)
        .anyMatch(
            accessControlEntry -> ((GrantedAuthoritySid) accessControlEntry.getSid()).getGrantedAuthority()
                .equals(authority));
    if (authorityHasAccess) {
      /* This is important!
       * Consider adding a person as admin and then adding them again as reader.
       * This leads to redundant access control entries.
       */
      throw new ApplicationException(
          "Authority %s already collaborates on %s. Please change the project role instead");
    }
    for (Permission permission : permissions) {
      aclForProject.insertAce(aclForProject.getEntries().size(), permission, authoritySid, true);
    }
    log.debug(
        "Authority %s now collaborates on project %s as %s.".formatted(authority, projectId.value(),
            projectRole.label()));
    aclService.updateAcl(aclForProject);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void removeAuthorityAccess(ProjectId projectId, String authority) {
    GrantedAuthoritySid grantedAuthoritySid = new GrantedAuthoritySid(authority);
    MutableAcl aclForProject = getAclForProject(projectId, List.of(grantedAuthoritySid),
        aclService);
    List<AccessControlEntry> entries = aclForProject.getEntries();
    for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
      AccessControlEntry accessControlEntry = entries.get(entryIndex);
      if (accessControlEntry.getSid().equals(grantedAuthoritySid)) {
        aclForProject.deleteAce(entryIndex);
      }
    }
    log.debug("Authority %s no longer collaborates on project %s.".formatted(authority,
        projectId.value()));
    aclService.updateAcl(aclForProject);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void changeAuthorityAccess(ProjectId projectId, String authority,
      ProjectRole projectRole) {
    GrantedAuthoritySid authoritySid = new GrantedAuthoritySid(authority);
    MutableAcl aclForProject = getAclForProject(projectId, List.of(authoritySid), aclService);

    Collection<Permission> requiredPermissions = projectRole.toPermissions();

    if (ProjectRole.OWNER.equals(projectRole)) {
      Sid previousOwner = aclForProject.getOwner();
      log.debug("Project %s owner changed from %s to authority %s".formatted(projectId.value(),
          previousOwner, projectId));
      aclForProject.setOwner(authoritySid);
      requiredPermissions = List.of();
    }

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
  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
  public void removeProject(ProjectId projectId) {
    ObjectIdentityImpl objectIdentity = new ObjectIdentityImpl(Project.class, projectId);
    aclService.deleteAcl(objectIdentity, true);
  }


}
