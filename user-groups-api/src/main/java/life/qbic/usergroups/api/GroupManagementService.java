package life.qbic.usergroups.api;

import java.io.Serializable;
import java.util.List;

/**
 * <b>Group management service (api facade)</b>
 *
 * <p>Access facade for the user groups context's <em>management</em> commands. This is the seam
 * the "My Groups" view and future management surfaces consume (the {@code GroupViewService}
 * established in Task A #1575). It exposes the group-internal management operations defined by
 * story FEAT-USER-GROUPS-04 owner/manager controls:</p>
 *
 * <ul>
 *   <li>add/remove regular members (OWNER + MANAGER)</li>
 *   <li>appoint/demote managers (OWNER only)</li>
 *   <li>rename/describe the group (OWNER + MANAGER)</li>
 *   <li>dissolve the group (OWNER only)</li>
 *   <li>list the members of a group (members only, per the visibility policy)</li>
 * </ul>
 *
 * <p>Like {@link GroupInformationService}, this interface is kept self-contained on purpose so
 * that {@code user-groups-api} never depends on the {@code user-groups} module. The caller
 * passes the acting user id explicitly; role gates are enforced by the application service.</p>
 *
 * @since 1.20.0
 */
public interface GroupManagementService extends Serializable {

  /**
   * Adds a regular member to a group.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER or MANAGER)
   * @param userId       the user to add as a regular member
   * @throws IllegalArgumentException if the group does not exist, the acting user lacks the
   *                                  required role, or the target is already a member
   * @since 1.20.0
   */
  void addMember(String groupId, String actingUserId, String userId);

  /**
   * Removes a regular member from a group.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the removal
   * @param userId       the member to remove
   * @throws IllegalArgumentException if the group does not exist or the acting user may not
   *                                  remove the given member
   * @since 1.20.0
   */
  void removeMember(String groupId, String actingUserId, String userId);

  /**
   * Appoints a member as a manager of a group.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @param userId       the member to promote to MANAGER
   * @throws IllegalArgumentException if the group does not exist or the acting user is not the
   *                                  OWNER
   * @since 1.20.0
   */
  void appointManager(String groupId, String actingUserId, String userId);

  /**
   * Demotes a manager back to a regular member.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @param userId       the manager to demote
   * @throws IllegalArgumentException if the group does not exist or the acting user is not the
   *                                  OWNER
   * @since 1.20.0
   */
  void demoteManager(String groupId, String actingUserId, String userId);

  /**
   * Renames a group.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER or MANAGER)
   * @param newName      the new group name (must be unique case-insensitively)
   * @throws IllegalArgumentException if the group does not exist, the acting user lacks the
   *                                  required role, or the new name is taken
   * @since 1.20.0
   */
  void renameGroup(String groupId, String actingUserId, String newName);

  /**
   * Updates the description of a group.
   *
   * @param groupId        the id of the group
   * @param actingUserId   the user performing the operation (must hold OWNER or MANAGER)
   * @param newDescription the new group description
   * @throws IllegalArgumentException if the group does not exist or the acting user lacks the
   *                                  required role
   * @since 1.20.0
   */
  void updateDescription(String groupId, String actingUserId, String newDescription);

  /**
   * Dissolves a group.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @throws IllegalArgumentException if the group does not exist or the acting user is not the
   *                                  OWNER
   * @since 1.20.0
   */
  void dissolveGroup(String groupId, String actingUserId);

  /**
   * Lists the members of a group.
   *
   * <p><b>Visibility:</b> only members of the requesting group (and QBiC admins for oversight)
   * can see its member list; the caller must be a member of the group.</p>
   *
   * @param groupId  the id of the group
   * @param viewerId the user requesting the list (must be a member of the group)
   * @return the group's members
   * @since 1.20.0
   */
  List<GroupMember> listMembers(String groupId, String viewerId);
}