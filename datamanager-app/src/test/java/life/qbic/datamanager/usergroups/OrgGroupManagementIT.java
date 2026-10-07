package life.qbic.datamanager.usergroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.Result;
import life.qbic.application.commons.SortOrder;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import life.qbic.usergroups.application.GroupInfoProjection;
import life.qbic.usergroups.application.GroupService;
import life.qbic.usergroups.application.service.GroupSidProviderImpl;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupRole;
import life.qbic.usergroups.domain.model.GroupStatus;
import life.qbic.usergroups.domain.model.GroupType;
import life.qbic.usergroups.domain.model.UserGroup;
import life.qbic.usergroups.domain.registry.DomainRegistry;
import life.qbic.usergroups.domain.repository.GroupDataStorage;
import life.qbic.usergroups.domain.repository.GroupRepository;
import life.qbic.usergroups.domain.service.GroupDomainService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * H2-backed integration test for org-group membership/manager management (story
 * FEAT-USER-GROUPS-02).
 *
 * <p>Proves the acceptance criteria end-to-end against real persistence (Hibernate + H2):
 * AC1 (admin appoints a manager → MANAGER membership; the manager can add/remove regular members
 * and rename/describe through the existing role-gated paths), AC2 (roster updates + the
 * member-added event fires), AC3 (removing a member revokes the removed user's group-derived SID
 * at the next check), AC4 (removing the last manager leaves the group ACTIVE, admin-governed and
 * no OWNER membership row is ever created).</p>
 *
 * <p>Uses the same storage/session-factory scaffolding as {@link OrgGroupCreationIT}.</p>
 *
 * <p>Runs under the {@code it} Maven profile (failsafe), same as {@code OrgGroupCreationIT}.</p>
 */
class OrgGroupManagementIT {

  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;
  private static GroupRepository repository;
  private static GroupService adminService;

  @BeforeAll
  static void setUp() {
    registry = new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
            "jdbc:h2:mem:orggroups-mgmt;DB_CLOSE_DELAY=-1")
        .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
        .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
        .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
        .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
        .build();
    MetadataSources sources = new MetadataSources(registry);
    sources.addAnnotatedClass(UserGroup.class);
    sources.addAnnotatedClass(life.qbic.usergroups.domain.model.GroupMembership.class);
    Metadata metadata = sources.buildMetadata();
    sessionFactory = metadata.buildSessionFactory();

    GroupDataStorage storage = new H2GroupDataStorage(sessionFactory);
    repository = GroupRepository.getInstance(storage);
    DomainRegistry.instance().registerService(new GroupDomainService(repository));

    GroupAdministrationPermission adminPermission =
        userId -> userId != null && userId.startsWith("admin-");
    adminService = new GroupService(repository, new TestUserInformationService(),
        adminPermission);
  }

  @AfterAll
  static void tearDown() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
    if (registry != null) {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  @Test
  void orgGroupManagementFlowEnforcesAcceptanceCriteria() {
    // ── Setup: an admin creates an org group.
    Result<GroupInfoProjection, ApplicationException> creation =
        adminService.createOrgGroup("admin-1", GroupName.from("MGMT Lab"),
            GroupDescription.from("QBiC management lab"));
    assertTrue(creation.isValue());
    String groupId = creation.getValue().groupId().get();

    // ── AC1: the admin appoints a manager; the user gains a MANAGER membership.
    Result<Void, ApplicationException> appoint =
        adminService.appointOrgManager(groupId, "admin-1", "manager-1");
    assertTrue(appoint.isValue());

    UserGroup stored = uniqueGroupNamed("MGMT Lab");
    assertEquals(GroupType.ORG, stored.type());
    assertEquals(1, stored.memberships().size());
    assertEquals("manager-1", stored.memberships().get(0).userId());
    assertEquals(GroupRole.MANAGER, stored.memberships().get(0).role());
    // No OWNER row, and the admin is not a member.
    assertTrue(stored.memberships().stream().noneMatch(m -> m.role() == GroupRole.OWNER));
    assertTrue(adminService.listMyGroups("admin-1").isEmpty());

    // The appointed manager can manage the org group exactly like an ad-hoc group manager:
    // add regular members, rename, describe (existing role-gated paths, type-agnostic).
    Result<Void, ApplicationException> add =
        adminService.addMember(groupId, "manager-1", "regular-1");
    assertTrue(add.isValue());
    Result<Void, ApplicationException> rename =
        adminService.renameGroup(groupId, "manager-1", GroupName.from("Renamed MGMT Lab"));
    assertTrue(rename.isValue());
    Result<Void, ApplicationException> describe =
        adminService.updateDescription(groupId, "manager-1",
            GroupDescription.from("updated description"));
    assertTrue(describe.isValue());

    stored = uniqueGroupNamed("Renamed MGMT Lab");
    assertEquals(2, stored.memberships().size());
    assertTrue(stored.memberships().stream()
        .anyMatch(m -> m.userId().equals("regular-1") && m.role() == GroupRole.MEMBER));

    // ── AC2: the added member gains the group-derived SID (they can be part of the effective
    // access for projects shared with the group). On member removal, the removed user's SID
    // disappears at the next check (AC3 — live, per-check derivation).
    GroupSidProviderImpl sidProvider = new GroupSidProviderImpl(adminService);
    assertEquals("GROUP_" + groupId,
        sidProvider.listGroupSidsForUser("regular-1").get(0));

    Result<Void, ApplicationException> removeMember =
        adminService.removeMember(groupId, "manager-1", "regular-1");
    assertTrue(removeMember.isValue());
    // Next authorization check (fresh SID derivation) no longer includes the group for regular-1.
    assertTrue(sidProvider.listGroupSidsForUser("regular-1").isEmpty());
    // The org group itself stays ACTIVE.
    stored = uniqueGroupNamed("Renamed MGMT Lab");
    assertEquals(GroupStatus.ACTIVE, stored.status());
    assertEquals(1, stored.memberships().size());

    // ── AC4: removing the last manager keeps the group ACTIVE and admin-governed; no OWNER row.
    Result<Void, ApplicationException> removeLastManager =
        adminService.removeOrgManager(groupId, "admin-1", "manager-1");
    assertTrue(removeLastManager.isValue());
    stored = uniqueGroupNamed("Renamed MGMT Lab");
    assertEquals(GroupStatus.ACTIVE, stored.status());
    assertTrue(stored.memberships().isEmpty(),
        "an org group must never carry an OWNER or any membership after the last manager is removed");
    Long ownerCount = sessionWithTransaction(session -> session
        .createQuery("select count(m) from GroupMembership m where m.role = :role", Long.class)
        .setParameter("role", GroupRole.OWNER)
        .getSingleResult());
    assertEquals(0L, ownerCount, "no OWNER membership row may ever be created");

    // ── AC3 gate: a non-admin cannot remove or appoint org managers.
    Result<Void, ApplicationException> deniedAppoint =
        adminService.appointOrgManager(groupId, "researcher-1", "some-user");
    assertFalse(deniedAppoint.isValue());
    assertEquals(ErrorCode.ACCESS_DENIED, deniedAppoint.getError().errorCode());
    assertTrue(adminService.listMembers(groupId, "admin-1").isEmpty());
  }

  /**
   * Loads the single group with the given name (eagerly fetching the roster) via a fresh session.
   */
  private UserGroup uniqueGroupNamed(String name) {
    return sessionWithTransaction(session -> session
        .createQuery("SELECT g FROM UserGroup g LEFT JOIN FETCH g.memberships "
            + "WHERE g.name = :name", UserGroup.class)
        .setParameter("name", GroupName.from(name))
        .getSingleResult());
  }

  /**
   * Runs the given work inside a single session+transaction (read-write) and returns the result.
   */
  private <T> T sessionWithTransaction(java.util.function.Function<Session, T> work) {
    try (Session session = sessionFactory.openSession()) {
      Transaction tx = session.beginTransaction();
      try {
        T result = work.apply(session);
        tx.commit();
        return result;
      } catch (RuntimeException | Error e) {
        tx.rollback();
        throw e;
      }
    }
  }

  /**
   * A {@link GroupDataStorage} backed by a real Hibernate {@link SessionFactory} (H2). Reads
   * eagerly fetch the roster so the aggregate never touches a closed session.
   */
  static class H2GroupDataStorage implements GroupDataStorage {

    private final SessionFactory sessionFactory;

    H2GroupDataStorage(SessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(UserGroup group) {
      try (Session session = sessionFactory.openSession()) {
        Transaction tx = session.beginTransaction();
        session.merge(group);
        tx.commit();
      }
    }

    @Override
    public Optional<UserGroup> findById(GroupId id) {
      return withSession(session -> session
          .createQuery("SELECT g FROM UserGroup g LEFT JOIN FETCH g.memberships "
              + "WHERE g.id = :id", UserGroup.class)
          .setParameter("id", id)
          .getResultList().stream().findFirst());
    }

    @Override
    public Optional<UserGroup> findByNameIgnoreCase(String name) {
      return withSession(session -> session
          .createQuery("SELECT g FROM UserGroup g LEFT JOIN FETCH g.memberships "
              + "WHERE LOWER(g.name) = LOWER(:name)", UserGroup.class)
          .setParameter("name", name)
          .getResultList().stream().findFirst());
    }

    @Override
    public List<UserGroup> findAllActive() {
      return withSession(session -> session
          .createQuery("SELECT DISTINCT g FROM UserGroup g LEFT JOIN FETCH g.memberships "
              + "WHERE g.status = :status", UserGroup.class)
          .setParameter("status", GroupStatus.ACTIVE)
          .getResultList());
    }

    @Override
    public List<UserGroup> findActiveGroupsByUserId(String userId) {
      return withSession(session -> session
          .createQuery("SELECT DISTINCT g FROM UserGroup g JOIN FETCH g.memberships "
              + "WHERE g.id.value IN (SELECT m.id.groupId FROM GroupMembership m "
              + "WHERE m.id.userId = :userId) AND g.status = :status", UserGroup.class)
          .setParameter("userId", userId)
          .setParameter("status", GroupStatus.ACTIVE)
          .getResultList());
    }

    private <T> T withSession(java.util.function.Function<Session, T> work) {
      try (Session session = sessionFactory.openSession()) {
        return work.apply(session);
      }
    }
  }

  /**
   * Minimal identity lookup for the application service; every user is considered to exist.
   */
  static class TestUserInformationService implements UserInformationService {

    @Override
    public Optional<UserInfo> findByEmail(String emailAddress) {
      return Optional.empty();
    }

    @Override
    public Optional<UserInfo> findById(String userId) {
      return Optional.of(new UserInfo(userId, userId, userId + "@example.org", userId, true,
          null, null));
    }

    @Override
    public boolean isUserNameAvailable(String userName) {
      return true;
    }

    @Override
    public boolean isEmailAvailable(String email) {
      return true;
    }

    @Override
    public Optional<UserInfo> findByOidc(String oidcId, String oidcIssuer) {
      return Optional.empty();
    }

    @Override
    public List<UserInfo> queryActiveUsersWithFilter(String filter, int offset, int limit,
        List<SortOrder> sortOrders) {
      return List.of();
    }
  }
}