package life.qbic.usergroups.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.UserGroup;

/**
 * <b>Group Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles {@link UserGroup} data storage.
 *
 * <p>This is the domain port of the user groups context. The actual persistence implementation
 * (JPA) is provided by the {@code user-groups-infrastructure} module and injected via the
 * singleton {@link GroupRepository}.
 *
 * @since 1.19.0
 */
public interface GroupDataStorage {

  /**
   * Saves a {@link UserGroup} entity permanently. Handles both creation and updates of an existing
   * group (the aggregate is identified by its stable {@link GroupId}).
   *
   * @param group the group to store
   * @since 1.19.0
   */
  void save(UserGroup group);

  /**
   * Finds a group based on its group id.
   *
   * @param id the group id to search for a matching entry in the storage
   * @return the group, or {@link Optional#empty()} if no group with the provided id was found
   * @since 1.19.0
   */
  Optional<UserGroup> findById(GroupId id);

  /**
   * Finds a group by its display name, ignoring case.
   *
   * <p>The underlying comparison depends on the database collation
   * ({@code utf8mb4_unicode_ci}) which is case-insensitive. This is the backing query for the
   * duplicate-name check of the application layer.
   *
   * @param name the group name to search for (case-insensitive)
   * @return the matching group, or {@link Optional#empty()} if no group with this name exists
   * @since 1.19.0
   */
  Optional<UserGroup> findByNameIgnoreCase(String name);

  /**
   * Returns all groups that are currently {@code ACTIVE}.
   *
   * <p>Used by the public directory: dissolved groups never appear.
   *
   * @return a list of active {@link UserGroup} entries
   * @since 1.19.0
   */
  List<UserGroup> findAllActive();

  /**
   * Returns all {@code ACTIVE} groups the given user is a member of.
   *
   * <p>Used by the "my groups" listing. A user's membership is matched by their user id in the
   * {@code group_membership} table.
   *
   * @param userId the user id to match memberships for
   * @return a list of active {@link UserGroup} entries the user belongs to
   * @since 1.19.0
   */
  List<UserGroup> findActiveGroupsByUserId(String userId);

}