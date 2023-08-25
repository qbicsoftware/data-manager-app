package life.qbic.authorization.acl;

import java.util.List;
import java.util.function.Predicate;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authorization.security.QbicUserDetails;
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
  public List<UserId> listUsers(ProjectId projectId) {
    List<String> userNames = listUsernames(projectId);
    return userNames.stream().map(userDetailsService::loadUserByUsername)
        .filter(it -> it instanceof QbicUserDetails)
        .map(it -> (QbicUserDetails) it)
        .map(QbicUserDetails::getUserId)
        .toList();
  }

  @Transactional
  @Override
  public List<String> listUsernames(ProjectId projectId) {
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
  public void grant(String username, ProjectId projectId, Permission permission) {
    PrincipalSid principalSid = new PrincipalSid(username);
    MutableAcl acl = getAclForProject(projectId, List.of(principalSid));
    acl.insertAce(acl.getEntries().size(), permission, principalSid, true);
    aclService.updateAcl(acl);
  }

  @Transactional
  @Override
  public void grantToAuthority(GrantedAuthority authority, ProjectId projectId,
      Permission permission) {
    GrantedAuthoritySid grantedAuthoritySid = new GrantedAuthoritySid(authority);
    MutableAcl acl = getAclForProject(projectId, List.of(grantedAuthoritySid));
    acl.insertAce(acl.getEntries().size(), permission, grantedAuthoritySid, true);
    aclService.updateAcl(acl);
  }

  @Transactional
  @Override
  public void deny(String username, ProjectId projectId, Permission permission) {
    PrincipalSid principalSid = new PrincipalSid(username);
    MutableAcl acl = getAclForProject(projectId, List.of(principalSid));
    Predicate<AccessControlEntry> accessControlEntryPredicate = accessControlEntry ->
        accessControlEntry.getSid().equals(principalSid)
            && accessControlEntry.getPermission().equals(permission);
    deleteAces(acl, accessControlEntryPredicate);
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

  @Transactional
  @Override
  public void denyAll(String username, ProjectId projectId) {
    PrincipalSid principalSid = new PrincipalSid(username);
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
