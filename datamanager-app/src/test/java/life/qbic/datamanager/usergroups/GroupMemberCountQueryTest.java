package life.qbic.datamanager.usergroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupMembership;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupStatus;
import life.qbic.usergroups.domain.model.UserGroup;
import life.qbic.usergroups.infrastructure.QbicGroupRepo;
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
 * H2-backed regression test for the group member count shown in the "My Groups" view.
 *
 * <p>The production "my groups" query
 * ({@link life.qbic.usergroups.infrastructure.QbicGroupRepo#findByMemberAndStatus}) must return
 * each matching group with its <em>complete</em> membership roster, because the application
 * layer derives the group's member count from {@code memberships().size()}.</p>
 *
 * <p>Regression: when the caller-membership predicate is applied to the fetch-join alias
 * ({@code JOIN FETCH g.memberships m WHERE m.id.userId = :userId}), the {@code :userId} WHERE
 * filter silently truncates the loaded collection to the single matching membership, so every
 * group reported a member count of 1. The predicate must instead be expressed as a correlated
 * {@code IN} subquery so the fetch join stays unfiltered and the full roster is hydrated.</p>
 *
 * <p>This test mirrors the JPQL of {@code QbicGroupRepo.findByMemberAndStatus} exactly; if the
 * production query ever regresses to a WHERE filter on the fetch-join alias again, the roster
 * assertion below fails.</p>
 */
class GroupMemberCountQueryTest {

  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    registry = new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
            "jdbc:h2:mem:groupcount;DB_CLOSE_DELAY=-1")
        .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
        .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
        .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
        .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
        .build();
    MetadataSources sources = new MetadataSources(registry);
    sources.addAnnotatedClass(UserGroup.class);
    sources.addAnnotatedClass(GroupMembership.class);
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
  void myGroupsQueryReturnsTheFullRosterOfEveryMatchingGroup() {
    Instant now = Instant.parse("2026-09-23T10:00:00Z");
    GroupId groupId = GroupId.create();

    // Seed a group with a full roster of three members. The caller (member-2) is
    // not the owner, so a truncated roster is clearly distinguishable from the real one.
    sessionFactory.inTransaction(session -> {
      UserGroup group = UserGroup.createAdHoc(groupId, GroupName.from("NGS Lab"),
          GroupDescription.from("A lab"), "owner-user", now);
      group.addMember("owner-user", "member-2", now);
      group.addMember("owner-user", "member-3", now);
      session.persist(group);
    });

    // The production query shape (QbicGroupRepo.findByMemberAndStatus): the caller's
    // membership is expressed via a correlated IN subquery, the fetch join is unfiltered.
    // Uses the shared repository constant so this test always mirrors the real JPQL.
    sessionFactory.inTransaction(session -> {
      List<UserGroup> result = session.createQuery(
              QbicGroupRepo.FIND_BY_MEMBER_AND_STATUS_QUERY, UserGroup.class)
          .setParameter("userId", "member-2")
          .setParameter("status", GroupStatus.ACTIVE)
          .getResultList();

      assertEquals(1, result.size(), "exactly the one matching group must be returned");
      UserGroup loaded = result.get(0);
      assertEquals(3, loaded.memberships().size(),
          "the fetched memberships collection must contain the complete roster, "
              + "not only the caller's own membership");
      List<String> userIds = loaded.memberships().stream()
          .map(GroupMembership::userId)
          .toList();
      assertTrue(userIds.containsAll(List.of("owner-user", "member-2", "member-3")),
          "all three members must be present, was: " + userIds);
    });
  }

  @Test
  void myGroupsQueryDoesNotReturnGroupsWithoutAMembershipOfTheCaller() {
    Instant now = Instant.parse("2026-09-23T10:00:00Z");

    // A group the caller does not belong to must not match the query.
    sessionFactory.inTransaction(session -> {
      UserGroup group = UserGroup.createAdHoc(GroupId.create(), GroupName.from("Foreign Lab"),
          GroupDescription.from("Not mine"), "stranger-user", now);
      group.addMember("stranger-user", "other-user", now);
      session.persist(group);
    });

    sessionFactory.inTransaction(session -> {
      List<UserGroup> result = session.createQuery(
              QbicGroupRepo.FIND_BY_MEMBER_AND_STATUS_QUERY, UserGroup.class)
          .setParameter("userId", "member-2")
          .setParameter("status", GroupStatus.ACTIVE)
          .getResultList();

      assertTrue(result.isEmpty(),
          "a group without a membership of the caller must never be returned");
    });
  }
}