package life.qbic.projectmanagement.application.authorization.acl;

import static java.util.function.Predicate.not;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.service.event.ProjectAccessGranted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAccessServiceImpl implements ProjectAccessService {

  private final MutableAclService aclService;
  private static final Logger log = logger(ProjectAccessServiceImpl.class);

  public ProjectAccessServiceImpl(@Autowired MutableAclService aclService) {
    this.aclService = aclService;
  }


  private static MutableAcl getAclForProject(ProjectId projectId, List<Sid> sids,
      MutableAclService mutableAclService) {
    ObjectIdentityImpl objectIdentity = new ObjectIdentityImpl(Project.class, projectId);
    MutableAcl acl;
    JdbcMutableAclService serviceImpl = (JdbcMutableAclService) mutableAclService;
    // these settings are necessary for MySQL to correctly throw several types of exceptions
    // instead of an unrelated exception related to the identity function
    serviceImpl.setClassIdentityQuery("SELECT @@IDENTITY");
    serviceImpl.setSidIdentityQuery("SELECT @@IDENTITY");
    try {
      acl = (MutableAcl) serviceImpl.readAclById(objectIdentity, sids);
    } catch (NotFoundException e) {
      acl = serviceImpl.createAcl(objectIdentity);
    }
    return acl;
  }

  private void fireProjectAccessGranted(String userId, ProjectId projectId) {
    var projectAccessGranted = ProjectAccessGranted.create(userId, projectId.value());
    DomainEventDispatcher.instance().dispatch(projectAccessGranted);
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
      fireProjectAccessGranted(userId, projectId);
      return;
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
    for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
      AccessControlEntry accessControlEntry = entries.get(entryIndex);
      if (accessControlEntry.getSid().equals(principalSid)) {
        aclForProject.deleteAce(entryIndex);
      }
    }
    log.debug("User %s no longer collaborates on project %s.".formatted(userId, projectId.value()));
    aclService.updateAcl(aclForProject);
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
  @Transactional(readOnly = true)
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'ADMINISTRATION')")
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
    // skip the owner as it is handled explicitly
    Set<ProjectCollaborator> otherCollaborators = entriesBySid.entrySet().stream()
        // skip the owner as it is handled explicitly
        .filter(sidListEntry -> !acl.getOwner().equals(sidListEntry.getKey()))
        .filter(sidListEntry -> sidListEntry.getKey() instanceof PrincipalSid)
        .map(sidListEntry -> {
          Set<Permission> permissions = sidListEntry.getValue().stream()
              .map(AccessControlEntry::getPermission)
              .collect(Collectors.toSet());
          ProjectRole projectRole = ProjectRole.fromPermissions(permissions).orElseThrow();
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
