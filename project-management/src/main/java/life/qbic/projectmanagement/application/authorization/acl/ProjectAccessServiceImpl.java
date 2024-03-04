package life.qbic.projectmanagement.application.authorization.acl;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAccessServiceImpl implements ProjectAccessService {

  private final MutableAclService aclService;

  public ProjectAccessServiceImpl(@Autowired MutableAclService aclService) {
    this.aclService = aclService;
  }

  @Override
  @Transactional
  public void addProjectCollaborator(ProjectId projectId, String userId, ProjectRole projectRole) {
    addProjectRole(projectId, userId, projectRole);
  }

  @Override
  @Transactional
  public void addProjectRole(ProjectId projectId, String userId, ProjectRole projectRole) {
    for (Permission permission : ProjectAccessService.getPermissions(projectRole)) {
      grant(userId, projectId, permission);
    }
  }

  @Override
  @Transactional
  public void replaceProjectRole(ProjectId projectId, String userId, ProjectRole oldRole,
      ProjectRole replacement) {
    for (Permission permission : ProjectAccessService.getPermissions(oldRole)) {
      deny(userId, projectId, permission);
    }
    for (Permission permission : ProjectAccessService.getPermissions(replacement)) {
      grant(userId, projectId, permission);
    }
  }

  @Override
  public void removeProjectRole(ProjectId projectId, String userId, ProjectRole projectRole) {
    for (Permission permission : ProjectAccessService.getPermissions(projectRole)) {
      deny(userId, projectId, permission);
    }
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void removeCollaborator(ProjectId projectId, String userId) {
    denyAll(userId, projectId);
  }
//
//  @Transactional
//  @Override
//  public List<String> listUserIds(ProjectId projectId) {
//    Acl acl = aclService.readAclById(new ObjectIdentityImpl(Project.class, projectId), null);
//    return acl.getEntries().stream()
//        .map(AccessControlEntry::getSid)
//        .filter(sid -> sid instanceof PrincipalSid)
//        .map(sid -> (PrincipalSid) sid)
//        .map(PrincipalSid::getPrincipal)
//        .toList();
//  }

  @Override
  public List<ProjectCollaborator> listCollaborators(ProjectId projectId) {
    Acl acl = aclService.readAclById(new ObjectIdentityImpl(Project.class, projectId), null);

    var collaborators = new ArrayList<ProjectCollaborator>();
    if (acl.getOwner() instanceof PrincipalSid principalSid) {
      ProjectCollaborator owner = new ProjectCollaborator(principalSid.getPrincipal(),
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
              .map(AccessControlEntry::getPermission).collect(Collectors.toSet());
          ProjectRole projectRole = ProjectAccessService.getRole(permissions);
          if (sidListEntry.getKey() instanceof PrincipalSid principalSid) {
            return new ProjectCollaborator(principalSid.getPrincipal(), projectRole);
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    collaborators.addAll(otherCollaborators);
    return collaborators;
  }

  @Transactional
  @Override
  public void grant(String userID, ProjectId projectId, Permission permission) {
    grant(userID, projectId, List.of(permission));
  }

  @Transactional
  @Override
  public void grant(String userID, ProjectId projectId, List<Permission> permissions) {
    PrincipalSid principalSid = new PrincipalSid(userID);
    MutableAcl acl = getAclForProject(projectId, List.of(principalSid));
    for (Permission permission : permissions) {
      acl.insertAce(acl.getEntries().size(), permission, principalSid, true);
    }
    aclService.updateAcl(acl);
  }

  @Transactional
  @Override
  public void grantToAuthority(GrantedAuthority authority, ProjectId projectId,
      Permission permission) {
    grantToAuthority(authority, projectId, List.of(permission));
  }

  @Transactional
  @Override
  public void grantToAuthority(GrantedAuthority authority, ProjectId projectId,
      List<Permission> permissions) {
    GrantedAuthoritySid grantedAuthoritySid = new GrantedAuthoritySid(authority);
    MutableAcl acl = getAclForProject(projectId, List.of(grantedAuthoritySid));
    for (Permission permission : permissions) {
      acl.insertAce(acl.getEntries().size(), permission, grantedAuthoritySid, true);
    }
    aclService.updateAcl(acl);
  }

  @Transactional
  @Override
  public void deny(String userID, ProjectId projectId, Permission permission) {
    deny(userID, projectId, List.of(permission));
  }

  @Transactional
  @Override
  public void deny(String userID, ProjectId projectId, List<Permission> permissions) {
    requireNonNull(userID, "userId must not be null");
    requireNonNull(projectId, "projectId must not be null");
    requireNonNull(permissions, "permissions must not be null");
    PrincipalSid principalSid = new PrincipalSid(userID);
    MutableAcl acl = getAclForProject(projectId, List.of(principalSid));
    Predicate<AccessControlEntry> accessControlEntryPredicate = accessControlEntry ->
        accessControlEntry.getSid().equals(principalSid)
            && permissions.contains(accessControlEntry.getPermission());
    deleteAces(acl, accessControlEntryPredicate);
  }

  @Transactional
  @Override
  public void denyAll(String userID, ProjectId projectId) {
    requireNonNull(userID, "userId must not be null");
    requireNonNull(projectId, "projectId must not be null");
    PrincipalSid principalSid = new PrincipalSid(userID);
    MutableAcl acl = getAclForProject(projectId, List.of(principalSid));
    deleteAces(acl, accessControlEntry -> accessControlEntry.getSid().equals(principalSid));
  }

  @Override
  public List<String> listAuthorities(ProjectId projectId) {
    Acl acl = aclService.readAclById(new ObjectIdentityImpl(Project.class, projectId), null);
    return acl.getEntries().stream()
        .map(AccessControlEntry::getSid)
        .filter(GrantedAuthoritySid.class::isInstance)
        .map(GrantedAuthoritySid.class::cast)
        .map(GrantedAuthoritySid::getGrantedAuthority)
        .toList();
  }

  private void deleteAces(MutableAcl mutableAcl,
      Predicate<AccessControlEntry> accessControlEntryPredicate) {
    List<AccessControlEntry> aclEntries = mutableAcl.getEntries();
    for (int i = aclEntries.size() - 1; i >= 0; i--) {
      if (accessControlEntryPredicate.test(aclEntries.get(i))) {
        mutableAcl.deleteAce(i);
      }
    }
    aclService.updateAcl(mutableAcl);
  }

  private MutableAcl getAclForProject(ProjectId projectId, List<Sid> sids) {
    ObjectIdentityImpl objectIdentity = new ObjectIdentityImpl(Project.class, projectId);
    MutableAcl acl;
    JdbcMutableAclService serviceImpl = (JdbcMutableAclService) aclService;
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

}
