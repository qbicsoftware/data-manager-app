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
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
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
 * H2-backed integration test for org-group creation (story FEAT-USER-GROUPS-01).
 *
 * <p>Proves the acceptance criteria end-to-end against real persistence (Hibernate + H2):
 * AC1 (an admin creates an org group → ACTIVE, in the public directory with type ORG),
 * AC2 (duplicate case-insensitive name rejected with the exact message) and AC3 (a non-admin is
 * denied and nothing is persisted). Crucially it asserts the <b>no OWNER row</b> invariant: an
 * org group is persisted with zero {@code group_membership} rows.</p>
 *
 * <p>Uses a single storage/session factory created once for the whole class so the singleton
 * {@link GroupRepository} (factored via {@code GroupRepository.getInstance}) is only ever bound
 * to the H2-backed storage of this test class — the factory is closed in {@code @AfterAll}.</p>
 *
 * <p>Runs under the {@code it} Maven profile (failsafe), same as
 * {@code UserGroupMembershipMappingIT}.</p>
 */
class OrgGroupCreationIT {

  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;
  private static GroupRepository repository;
  private static GroupService service;
  private static GroupService adminService;

  @BeforeAll
  static void setUp() {
    registry = new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
            "jdbc:h2:mem:orggroups;DB_CLOSE_DELAY=-1")
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
    service = new GroupService(repository, new TestUserInformationService());
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
  void orgGroupCreationFlowEnforcesAllAcceptanceCriteria() {
    // ── AC1: an admin creates an org group with a unique case-insensitive name + description.
    Result<GroupInfoProjection, ApplicationException> result =
        adminService.createOrgGroup("admin-1", GroupName.from("NGS Lab"),
            GroupDescription.from("QBiC NGS laboratory"));

    assertTrue(result.isValue(), "admin must be able to create an org group");
    GroupInfoProjection created = result.getValue();
    assertEquals(GroupType.ORG, created.groupType());

    // Reload: ACTIVE status with type ORG in the storage (roster eagerly fetched).
    UserGroup stored = uniqueGroupNamed("NGS Lab");
    assertEquals(GroupStatus.ACTIVE, stored.status());
    assertEquals(GroupType.ORG, stored.type());
    assertEquals("admin-1", stored.createdBy());

    // The org group surfaces in the public directory.
    List<GroupInfoProjection> directory = service.listPublicDirectory();
    assertEquals(1, directory.size());
    assertEquals("NGS Lab", directory.get(0).groupName().value());
    assertEquals(GroupType.ORG, directory.get(0).groupType());

    // D4/strategy: an org group has NO OWNER membership row.
    assertTrue(stored.memberships().isEmpty(),
        "an org group must never carry an OWNER membership row");
    // And the creating admin is not a member ("my groups" stays empty).
    assertTrue(service.listMyGroups("admin-1").isEmpty());

    // ── AC2: an existing org name blocks a second org group (case-different form).
    Result<GroupInfoProjection, ApplicationException> duplicate =
        adminService.createOrgGroup("admin-1", GroupName.from("ngs lab"),
            GroupDescription.from("lab"));
    assertFalse(duplicate.isValue());
    assertEquals(ErrorCode.DUPLICATE_GROUP_NAME, duplicate.getError().errorCode());
    assertEquals(
        "A group with the name 'ngs lab' already exists. "
            + "Group names must be unique (case-insensitive).",
        duplicate.getError().getMessage());
    // Nothing further was persisted.
    assertEquals(1, service.listPublicDirectory().size());

    // ── AC3: a non-admin cannot create an org group.
    Result<GroupInfoProjection, ApplicationException> denied =
        adminService.createOrgGroup("researcher-1", GroupName.from("Secrets Lab"),
            GroupDescription.from("must not exist"));
    assertFalse(denied.isValue());
    assertEquals(ErrorCode.ACCESS_DENIED, denied.getError().errorCode());

    // Only the original org group row exists — nothing from AC3 was persisted.
    assertEquals(1, service.listPublicDirectory().size());
    Long count = sessionWithTransaction(session -> session
        .createQuery("select count(g) from UserGroup g", Long.class)
        .getSingleResult());
    assertEquals(1L, count);
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