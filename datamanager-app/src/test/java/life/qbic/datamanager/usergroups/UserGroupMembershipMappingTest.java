package life.qbic.datamanager.usergroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupRole;
import life.qbic.usergroups.domain.model.GroupStatus;
import life.qbic.usergroups.domain.model.UserGroup;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * H2-backed mapping test for the user-groups aggregate.
 *
 * <p>Verifies that the {@code @OneToMany(mappedBy = "group")} / {@code @ManyToOne(insertable =
 * false, updatable = false)} mapping on {@link UserGroup}/{@link GroupMembership} produces
 * correct DELETE SQL for orphan removal (regression for "No value specified for parameter 2").</p>
 */
class UserGroupMembershipMappingIT {

  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    registry = new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:h2:mem:groupstest;DB_CLOSE_DELAY=-1")
        .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
        .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
        .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
        .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
        .applySetting(AvailableSettings.SHOW_SQL, true)
        .applySetting(AvailableSettings.FORMAT_SQL, true)
        .build();
    MetadataSources sources = new MetadataSources(registry);
    sources.addAnnotatedClass(UserGroup.class);
    sources.addAnnotatedClass(life.qbic.usergroups.domain.model.GroupMembership.class);
    Metadata metadata = sources.buildMetadata();
    sessionFactory = metadata.buildSessionFactory();
  }

  @AfterEach
  void tearDown() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
    if (registry != null) {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  @Test
  void removingAMemberDeletesByCompositeKeyAndLeavesOthers() {
    GroupId groupId = GroupId.create();
    Instant now = Instant.parse("2026-09-23T10:00:00Z");

    // persist a group with two members via the aggregate
    sessionFactory.inTransaction(session -> {
      UserGroup group = createGroupWithMembers(groupId, now);
      session.persist(group);
    });

    // reload and remove one member (orphan removal DELETE path)
    sessionFactory.inTransaction(session -> {
      UserGroup managed = session.find(UserGroup.class, groupId);
      managed.removeMember("owner-user", "member-2");
      session.flush();
    });

    // verify the remaining roster via a fresh session
    sessionFactory.inTransaction(session -> {
      UserGroup reloaded = session.find(UserGroup.class, groupId);
      assertEquals(1, reloaded.memberships().size(),
          "removing one member must leave the other membership intact");
      assertEquals("owner-user", reloaded.memberships().get(0).userId());
      assertEquals(GroupStatus.ACTIVE, reloaded.status());
    });
  }

  @Test
  void removingTheLastMemberDissolvesTheGroupAndClearsRoster() {
    GroupId groupId = GroupId.create();
    Instant now = Instant.parse("2026-09-23T10:00:00Z");

    sessionFactory.inTransaction(session -> {
      UserGroup group = createGroupWithMembers(groupId, now);
      session.persist(group);
    });

    sessionFactory.inTransaction(session -> {
      UserGroup managed = session.find(UserGroup.class, groupId);
      managed.removeMember("owner-user", "member-2");
      session.flush();
    });

    sessionFactory.inTransaction(session -> {
      UserGroup reloaded = session.find(UserGroup.class, groupId);
      reloaded.removeMembership("owner-user");
      session.flush();
    });

    sessionFactory.inTransaction(session -> {
      UserGroup dissolved = session.find(UserGroup.class, groupId);
      assertEquals(GroupStatus.DISSOLVED, dissolved.status());
      assertTrue(dissolved.memberships().isEmpty(),
          "a dissolved group must have no memberships left");
      assertEquals(List.of(), dissolved.memberships(), "memberships must be purged");
    });
  }

  private static UserGroup createGroupWithMembers(GroupId groupId, Instant now) {
    UserGroup group = UserGroup.createAdHoc(groupId, GroupName.from("NGS Lab"),
        GroupDescription.from("A test lab"), "owner-user", now);
    group.addMember("owner-user", "member-2", now);
    return group;
  }
}