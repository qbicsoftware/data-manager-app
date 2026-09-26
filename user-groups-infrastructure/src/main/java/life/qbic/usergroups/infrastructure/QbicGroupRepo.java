package life.qbic.usergroups.infrastructure;


import java.util.List;
import java.util.Optional;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupStatus;
import life.qbic.usergroups.domain.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <b>QBiC user group repository interface</b>
 *
 * <p>This interface will be automatically detected by Spring on application startup and create an
 * instance of this class automatically.</p>
 *
 * <p>Since it extends the {@link JpaRepository} class from Spring, CRUD operations are provided
 * out of the box. Entity attribute lookups matching the persistence port of the user groups
 * context are declared as derived queries or explicit JPQL.</p>
 *
 * <p>The case-insensitive uniqueness of {@code name} is enforced by the database unique index on
 * {@code user_group.name} under the {@code utf8mb4_unicode_ci} collation — see
 * {@code sql/migrations/create-user-groups.sql}.</p>
 *
 * @since 1.19.0
 */
public interface QbicGroupRepo extends JpaRepository<UserGroup, GroupId> {

  /**
   * Finds a group by its display name, ignoring case (the underlying comparison additionally
   * benefits from the {@code utf8mb4_unicode_ci} column collation).
   *
   * <p>Explicit JPQL: the {@code name} column is a {@code GroupName} value object (with an
   * {@code @Convert}), so Spring Data's derived {@code ...IgnoreCase} keyword (which only
   * supports {@code String} properties) cannot be used; the case-insensitive comparison is
   * expressed via {@code LOWER} on both sides.
   *
   * @param name the group name to search for
   * @return the matching group, or {@link Optional#empty()} if none matches
   * @since 1.19.0
   */
  @Query("SELECT g FROM UserGroup g WHERE LOWER(g.name) = LOWER(:name)")
  Optional<UserGroup> findByNameIgnoreCase(@Param("name") String name);

  /**
   * Returns all groups with the given lifecycle status.
   *
   * @param status the status to filter for
   * @return groups in the given status
   * @since 1.19.0
   */
  List<UserGroup> findByStatus(GroupStatus status);

  /**
   * The JPQL of {@link #findByMemberAndStatus}, extractable so tests can execute the exact
   * production query (instead of re-typing it and risking drift). The query keeps the
   * {@code JOIN FETCH} unfiltered and expresses the caller membership via a correlated
   * {@code IN} subquery — see {@link #findByMemberAndStatus} for why.
   *
   * @since 1.19.0
   */
  String FIND_BY_MEMBER_AND_STATUS_QUERY = "SELECT DISTINCT g FROM UserGroup g JOIN FETCH g.memberships "
      + "WHERE g.id.value IN (SELECT m.id.groupId FROM GroupMembership m "
      + "WHERE m.id.userId = :userId) AND g.status = :status";

  /**
   * Returns all groups in the given status that contain a membership of the provided user.
   *
   * <p>Explicit JPQL navigation through the aggregate's membership collection: each membership's
   * composite key embeds the {@code userId}, so the {@code group_membership.user_id} column is
   * reached via {@code m.id.userId}.</p>
   *
   * <p><b>Important:</b> the membership predicate must <em>not</em> be applied to the
   * {@code JOIN FETCH g.memberships} alias. A fetch join whose alias appears in a WHERE clause
   * silently truncates the loaded collection to the matching rows — the fetched
   * {@code memberships} collection would then contain only the caller's own membership, making
   * {@code memberships().size()} (the roster/member count shown in the My Groups view) report
   * 1 for every group. The caller's membership is therefore expressed with a correlated
   * {@code IN} subquery on the composite id, while the fetch join stays unfiltered so the full
   * roster is hydrated.</p>
   *
   * @param userId the user id to match memberships for
   * @param status the group status to filter for (typically {@link GroupStatus#ACTIVE})
   * @return the matching groups with their complete membership rosters
   * @since 1.19.0
   */
  @Query(FIND_BY_MEMBER_AND_STATUS_QUERY)
  List<UserGroup> findByMemberAndStatus(@Param("userId") String userId,
      @Param("status") GroupStatus status);
}