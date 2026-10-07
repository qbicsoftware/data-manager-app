package life.qbic.datamanager.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.projectmanagement.application.authorization.acl.QbicPermissionEvaluator;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.usergroups.api.GroupSidProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * End-to-end proof of the user-group sharing authorization path (story FEAT-USER-GROUPS-06).
 *
 * <p>Wires the real {@link QbicPermissionEvaluator} with the {@link GroupAwareSidRetrievalStrategy}
 * against an in-memory ACL store backed by Spring's {@link AclImpl} and proves the group-aware
 * decision path: a group member gains the granted project role at the next permission check
 * (AC1), non-members do not (AC5/§5.1) and the OWNER invariant is honoured because a group sid is
 * never the ACL owner (AC3).</p>
 */
class GroupShareAuthorizationTest {

  private QbicPermissionEvaluator permissionEvaluator;
  private InMemoryAclService inMemoryAclService;

  @BeforeEach
  void setup() {
    inMemoryAclService = new InMemoryAclService();

    GroupSidProvider groupSidProvider = userId -> userId.equals("member-1")
        ? List.of("GROUP_group-1") : List.of();
    AuthenticationToUserIdTranslator translator =
        authentication -> Optional.ofNullable(authentication.getPrincipal())
            .map(Object::toString);
    GroupAwareSidRetrievalStrategy strategy = new GroupAwareSidRetrievalStrategy(
        groupSidProvider, translator);
    permissionEvaluator = new QbicPermissionEvaluator(inMemoryAclService);
    permissionEvaluator.setSidRetrievalStrategy(strategy);

    // ACL mutation (insertAce/deleteAce/setOwner) requires an authenticated principal with the
    // ACL mutation authorities
    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("owner-1", "x",
            List.of(new SimpleGrantedAuthority("acl:change-owner"),
                new SimpleGrantedAuthority("acl:change-audit"),
                new SimpleGrantedAuthority("acl:change-access"))));
  }

  /** Creates an ACL with a principal owner plus one group grant (group role never OWNER). */
  private ProjectId createProjectSharedWithGroup(String ownerId, String groupId,
      Permission... groupPermissions) {
    ProjectId projectId = ProjectId.create();
    ObjectIdentityImpl identity = new ObjectIdentityImpl(Project.class, projectId.value());
    var acl = inMemoryAclService.createAcl(identity, new PrincipalSid(ownerId));

    PrincipalSid ownerSid = new PrincipalSid(ownerId);
    for (Permission permission : List.of(BasePermission.READ, BasePermission.WRITE,
        BasePermission.ADMINISTRATION, BasePermission.CREATE, BasePermission.DELETE)) {
      acl.insertAce(acl.getEntries().size(), permission, ownerSid, true);
    }

    GrantedAuthoritySid groupSid = new GrantedAuthoritySid("GROUP_" + groupId);
    for (Permission permission : groupPermissions) {
      acl.insertAce(acl.getEntries().size(), permission, groupSid, true);
    }
    inMemoryAclService.updateAcl(acl);
    return projectId;
  }

  private Authentication member(String userId) {
    return new UsernamePasswordAuthenticationToken(userId, "x",
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private boolean has(Authentication authentication, ProjectId projectId, Permission permission) {
    return permissionEvaluator.hasPermission(authentication, projectId,
        "life.qbic.projectmanagement.domain.model.project.Project", permission);
  }

  @Test
  void groupMemberGainsTheGrantedProjectRoleAtTheNextPermissionCheck() {
    // given: a project shared with a group at WRITE
    ProjectId projectId = createProjectSharedWithGroup("owner-1", "group-1",
        BasePermission.READ, BasePermission.WRITE);

    // then: the member of the group has READ + WRITE but NOT ADMINISTRATION
    assertTrue(has(member("member-1"), projectId, BasePermission.READ));
    assertTrue(has(member("member-1"), projectId, BasePermission.WRITE));
    assertFalse(has(member("member-1"), projectId, BasePermission.ADMINISTRATION));
  }

  @Test
  void ADMINGroupGrantsAdministrationPermission() {
    ProjectId projectId = createProjectSharedWithGroup("owner-1", "group-1",
        BasePermission.READ, BasePermission.WRITE, BasePermission.ADMINISTRATION);

    assertTrue(has(member("member-1"), projectId, BasePermission.ADMINISTRATION));
    assertTrue(has(member("member-1"), projectId, BasePermission.READ));
  }

  @Test
  void nonMembersDoNotGainGroupAccess() {
    ProjectId projectId = createProjectSharedWithGroup("owner-1", "group-1",
        BasePermission.READ, BasePermission.WRITE, BasePermission.ADMINISTRATION);

    assertFalse(has(member("someone-else"), projectId, BasePermission.READ));
  }

  @Test
  void ownerInvariantHoldsAGroupNevetBecomesTheProjectOwner() {
    ProjectId projectId = createProjectSharedWithGroup("owner-1", "group-1",
        BasePermission.READ);

    Acl acl = inMemoryAclService.readAclById(new ObjectIdentityImpl(Project.class,
        projectId.value()));
    assertTrue(acl.getOwner() instanceof PrincipalSid,
        "A group must never become the project OWNER (AC3) — only principals own projects");
  }

  @Test
  void groupAdminMemberCanDemoteTheirOwnGroupRole() {
    // given: the project is shared with a group at ADMIN (READ+WRITE+ADMINISTRATION)
    // and the acting user is a member of that very group (demoting their own group)
    ProjectId projectId = createProjectSharedWithGroup("owner-1", "group-1",
        BasePermission.READ, BasePermission.WRITE, BasePermission.ADMINISTRATION);

    // authenticate as a GROUP member WITHOUT the raw acl:change-access authority
    // (the repro for FEAT-USER-GROUPS-08: a group-admin member changing their own group)
    SecurityContextHolder.getContext().setAuthentication(member("member-1"));

    // when: the member demotes their own group from ADMIN -> READ
    Acl acl = inMemoryAclService.readAclById(
        new ObjectIdentityImpl(Project.class, projectId.value()));
    GrantedAuthoritySid groupSid = new GrantedAuthoritySid("GROUP_group-1");
    MutableAcl mutableAcl = (MutableAcl) acl;
    List<AccessControlEntry> groupEntries = acl.getEntries().stream()
        .filter(entry -> entry.getSid().equals(groupSid))
        .toList();
    for (AccessControlEntry entry : groupEntries) {
      if (entry.getPermission().equals(BasePermission.ADMINISTRATION)
          || entry.getPermission().equals(BasePermission.WRITE)) {
        mutableAcl.deleteAce(acl.getEntries().indexOf(entry));
      }
    }

    // then: the demotion succeeds without "Unable to locate a matching ACE"
    // (the ACL authorization check inside deleteAce must see the caller's GROUP_group-1 sid)
    assertFalse(
        permissionEvaluator.hasPermission(member("member-1"), projectId,
            "life.qbic.projectmanagement.domain.model.project.Project",
            BasePermission.ADMINISTRATION),
        "after demotion the group member must no longer hold ADMINISTRATION");
  }

  /**
   * Minimal in-memory {@link MutableAclService} using Spring's real {@link AclImpl} so the
   * evaluator exercises the genuine {@code isGranted} decision path without a database.
   */
  private static final class InMemoryAclService implements MutableAclService {

    private final Map<String, MutableAcl> aclsByKey = new java.util.HashMap<>();

    @Override
    public MutableAcl readAclById(ObjectIdentity object, List<Sid> sids) throws NotFoundException {
      var acl = aclsByKey.get(key(object));
      if (acl == null) {
        throw new NotFoundException("No ACL for " + object);
      }
      return acl;
    }

    @Override
    public MutableAcl readAclById(ObjectIdentity object) throws NotFoundException {
      return readAclById(object, List.of());
    }

    @Override
    public List<ObjectIdentity> findChildren(ObjectIdentity parentIdentity) {
      return List.of();
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects) {
      return readAclsById(objects, List.of());
    }

    @Override
    public Map<ObjectIdentity, Acl> readAclsById(List<ObjectIdentity> objects, List<Sid> sids) {
      Map<ObjectIdentity, Acl> result = new java.util.HashMap<>();
      objects.forEach(object -> {
        var acl = aclsByKey.get(key(object));
        if (acl != null) {
          result.put(object, acl);
        }
      });
      return result;
    }

    @Override
    public MutableAcl createAcl(ObjectIdentity objectIdentity) throws AlreadyExistsException {
      return createAcl(objectIdentity, null);
    }

    MutableAcl createAcl(ObjectIdentity objectIdentity, Sid owner) throws AlreadyExistsException {
      if (aclsByKey.containsKey(key(objectIdentity))) {
        throw new AlreadyExistsException("ACL exists for " + objectIdentity);
      }
      var acl = newAcl(objectIdentity, owner);
      aclsByKey.put(key(objectIdentity), acl);
      return acl;
    }

    @Override
    public void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren) {
      aclsByKey.remove(key(objectIdentity));
    }

    @Override
    public MutableAcl updateAcl(MutableAcl acl) {
      aclsByKey.put(key(acl.getObjectIdentity()), acl);
      return acl;
    }

    private static MutableAcl newAcl(ObjectIdentity identity, Sid owner) {
      AuditLogger auditLogger = new ConsoleAuditLogger();
      AclAuthorizationStrategyImpl authorizationStrategy = new AclAuthorizationStrategyImpl(
          new SimpleGrantedAuthority("acl:change-owner"),
          new SimpleGrantedAuthority("acl:change-audit"),
          new SimpleGrantedAuthority("acl:change-access"));
      // mirror the production fix: the ACL authorization strategy must resolve group sids too,
      // otherwise a group-admin member cannot deleteAce/insertAce ("Unable to locate a matching
      // ACE") — see AclSecurityConfiguration.aclAuthorizationStrategy()
      GroupAwareSidRetrievalStrategy groupAware = new GroupAwareSidRetrievalStrategy(
          userId -> userId.equals("member-1") ? List.of("GROUP_group-1") : List.of(),
          authentication -> Optional.ofNullable(authentication.getPrincipal())
              .map(Object::toString));
      authorizationStrategy.setSidRetrievalStrategy(groupAware);
      return new AclImpl(identity, identity.getIdentifier(), authorizationStrategy,
          new DefaultPermissionGrantingStrategy(auditLogger), null, null, true, owner);
    }

    private static String key(ObjectIdentity objectIdentity) {
      return objectIdentity.getType() + ":" + objectIdentity.getIdentifier();
    }
  }
}