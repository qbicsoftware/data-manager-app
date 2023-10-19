package life.qbic.projectmanagement.application.authorization.acl;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Predicate;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAccessServiceImpl implements ProjectAccessService {

  private final MutableAclService aclService;
  private final UserDetailsService userDetailsService;

  public ProjectAccessServiceImpl(@Autowired MutableAclService aclService,
      @Autowired UserDetailsService userDetailsService) {
    this.aclService = aclService;
    this.userDetailsService = userDetailsService;
  }

  @Transactional
  @Override
  public List<String> listUserIds(ProjectId projectId) {
    Acl acl = aclService.readAclById(new ObjectIdentityImpl(Project.class, projectId), null);
    return acl.getEntries().stream()
        .map(AccessControlEntry::getSid)
        .filter(sid -> sid instanceof PrincipalSid)
        .map(sid -> (PrincipalSid) sid)
        .map(PrincipalSid::getPrincipal)
        .toList();
  }

  @Transactional
  @Override
  public List<String> listActiveUserIds(ProjectId projectId) {
    return listUserIds(projectId).stream().distinct().toList();
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
        .filter(sid -> sid instanceof GrantedAuthoritySid)
        .map(sid -> (GrantedAuthoritySid) sid)
        .map(GrantedAuthoritySid::getGrantedAuthority)
        .toList();
  }

  @Override
  public List<String> listAuthoritiesForPermission(ProjectId projectId, Permission permission) {
    Acl acl = aclService.readAclById(new ObjectIdentityImpl(Project.class, projectId), null);
    return acl.getEntries().stream()
        .filter(accessControlEntry -> accessControlEntry.getPermission().equals(permission))
        .map(AccessControlEntry::getSid)
        .filter(sid -> sid instanceof GrantedAuthoritySid)
        .map(sid -> (GrantedAuthoritySid) sid)
        .map(GrantedAuthoritySid::getGrantedAuthority)
        .toList();
  }

  private void deleteAces(MutableAcl mutableAcl,
      Predicate<AccessControlEntry> accessControlEntryPredicate) {
    List<AccessControlEntry> aclEntries = mutableAcl.getEntries();
    for (int i = 0; i < aclEntries.size(); i++) {
      if (accessControlEntryPredicate.test(aclEntries.get(i))) {
        mutableAcl.deleteAce(i);
      }
    }
    aclService.updateAcl(mutableAcl);
  }

  private MutableAcl getAclForProject(ProjectId projectId, List<Sid> sids) {
    ObjectIdentityImpl objectIdentity = new ObjectIdentityImpl(Project.class, projectId);
    MutableAcl acl;
    try {
      acl = (MutableAcl) aclService.readAclById(objectIdentity, sids);
    } catch (NotFoundException e) {
      acl = aclService.createAcl(objectIdentity);
    }
    return acl;
  }

}
