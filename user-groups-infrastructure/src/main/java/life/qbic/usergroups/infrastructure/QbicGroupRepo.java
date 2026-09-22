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
   * @param name the group name to search for
   * @return the matching group, or {@link Optional#empty()} if none matches
   * @since 1.19.0
   */
  Optional<UserGroup> findByNameIgnoreCase(String name);

  /**
   * Returns all groups with the given lifecycle status.
   *
   * @param status the status to filter for
   * @return groups in the given status
   * @since 1.19.0
   */
  List<UserGroup> findByStatus(GroupStatus status);

  /**
   * Returns all groups in the given status that contain a membership of the provided user.
   *
   * <p>Explicit JPQL navigation through the aggregate's membership collection: each membership's
   * composite key embeds the {@code userId}, so the {@code group_membership.user_id} column is
   * reached via {@code m.id.userId}.</p>
   *
   * @param userId the user id to match memberships for
   * @param status the group status to filter for (typically {@link GroupStatus#ACTIVE})
   * @return the matching groups
   * @since 1.19.0
   */
  @Query("SELECT DISTINCT g FROM UserGroup g JOIN FETCH g.memberships m "
      + "WHERE m.id.userId = :userId AND g.status = :status")
  List<UserGroup> findByMemberAndStatus(@Param("userId") String userId,
      @Param("status") GroupStatus status);
}