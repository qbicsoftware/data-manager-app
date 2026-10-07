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
 * @since 1.19.0
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
   * @since 1.19.0
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
   * @since 1.19.0
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
   * @since 1.19.0
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
   * @since 1.19.0
   */
  void demoteManager(String groupId, String actingUserId, String userId);

  /**
   * Appoints a user as a MANAGER of an <b>org</b> group (QBiC admin governed).
   *
   * <p>Only a QBiC administrator (enforced via the admin-gate port at the application
   * boundary) may appoint org-group managers. The appointed user may be a non-member (direct
   * appointment) or an existing regular MEMBER (promotion). Org groups have no OWNER row; no
   * OWNER membership is ever created.</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId        the user to appoint (non-member or existing MEMBER)
   * @throws IllegalArgumentException if the group does not exist, is not an org group, the
   *                                  acting user is not a QBiC administrator, or the target
   *                                  user is unknown
   * @since 1.19.0
   */
  void appointOrgManager(String groupId, String actingAdminUserId, String userId);

  /**
   * Removes a manager's (or member's) membership from an <b>org</b> group (QBiC admin
   * governed).
   *
   * <p>Only a QBiC administrator may remove an org-group manager. The member's membership is
   * removed entirely (no demotion). Removing the last manager keeps the group ACTIVE and
   * admin-governed (no OWNER row).</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId        the manager (or member) to remove
   * @throws IllegalArgumentException if the group does not exist, is not an org group, the
   *                                  acting user is not a QBiC administrator, or the target is
   *                                  not a member
   * @since 1.19.0
   */
  void removeOrgManager(String groupId, String actingAdminUserId, String userId);

  /**
   * Adds a regular MEMBER to an <b>org</b> group (QBiC admin governed).
   *
   * <p>Only a QBiC administrator may add a regular member (the admin is owner-equivalent;
   * org MANAGERs use the role-gated {@link #addMember} path).</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId        the user to add as a regular member
   * @throws IllegalArgumentException if the group does not exist, is not an org group, the
   *                                  acting user is not a QBiC administrator, or the target
   *                                  user is unknown/already a member
   * @since 1.19.0
   */
  void addOrgMember(String groupId, String actingAdminUserId, String userId);

  /**
   * Removes a regular MEMBER (or manager) membership from an <b>org</b> group (QBiC admin
   * governed).
   *
   * <p>Only a QBiC administrator may remove an org-group member. Removing the last manager
   * keeps the group ACTIVE and admin-governed (no OWNER row).</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId        the member (or manager) to remove
   * @throws IllegalArgumentException if the group does not exist, is not an org group, the
   *                                  acting user is not a QBiC administrator, or the target is
   *                                  not a member
   * @since 1.19.0
   */
  void removeOrgMember(String groupId, String actingAdminUserId, String userId);

  /**
   * Demotes a MANAGER back to a regular MEMBER of an <b>org</b> group (QBiC admin governed).
   *
   * <p>Only a QBiC administrator may demote an org-group manager. The manager stays in the
   * group as a MEMBER (role assignment in the group roster); this is distinct from
   * {@link #removeOrgMember} which removes the membership entirely.</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId        the manager to demote
   * @throws IllegalArgumentException if the group does not exist, is not an org group, the
   *                                  acting user is not a QBiC administrator, or the target is
   *                                  not a manager
   * @since 1.19.0
   */
  void demoteOrgManager(String groupId, String actingAdminUserId, String userId);

  /**
   * Returns the member count of an <b>org</b> group (QBiC admin oversight).
   *
   * <p>Only a QBiC administrator may request the roster size of an org group; the public
   * directory never carries membership data. The count reuses the same semantics as the
   * member-count badge in My Groups (derived from the roster, members with any role).</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user requesting the count (must be a QBiC administrator)
   * @return the number of members in the org group, or {@code 0} if the group does not exist,
   * is not an active org group, or the caller is not a QBiC administrator
   * @throws IllegalArgumentException if the acting user id is blank
   * @since 1.19.0
   */
  int orgGroupMemberCount(String groupId, String actingAdminUserId);

  /**
   * Renames an <b>org</b> group (QBiC admin governed).
   *
   * <p>Only a QBiC administrator may rename an org group. The new name must be unique
   * (case-insensitive).</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param newName       the new group name (must be unique case-insensitively)
   * @throws IllegalArgumentException if the group does not exist, is not an org group, the
   *                                  acting user is not a QBiC administrator, or the new name
   *                                  is taken
   * @since 1.19.0
   */
  void renameOrgGroup(String groupId, String actingAdminUserId, String newName);

  /**
   * Updates the description of an <b>org</b> group (QBiC admin governed).
   *
   * <p>Only a QBiC administrator may update the description of an org group.</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param newDescription the new group description
   * @throws IllegalArgumentException if the group does not exist, is not an org group, or the
   *                                  acting user is not a QBiC administrator
   * @since 1.19.0
   */
  void updateOrgGroupDescription(String groupId, String actingAdminUserId, String newDescription);

  /**
   * Dissolves an <b>org</b> group (QBiC admin governed).
   *
   * <p>Only a QBiC administrator may dissolve an org group (admin dissolve is the only way an
   * org group ends — it never auto-dissolves). The removed members lose their group-derived
   * project access at the next authorization check.</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @throws IllegalArgumentException if the group does not exist, is not an org group, or the
   *                                  acting user is not a QBiC administrator
   * @since 1.19.0
   */
  void dissolveOrgGroup(String groupId, String actingAdminUserId);

  /**
   * Renames a group.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER or MANAGER)
   * @param newName      the new group name (must be unique case-insensitively)
   * @throws IllegalArgumentException if the group does not exist, the acting user lacks the
   *                                  required role, or the new name is taken
   * @since 1.19.0
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
   * @since 1.19.0
   */
  void updateDescription(String groupId, String actingUserId, String newDescription);

  /**
   * Dissolves a group.
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @throws IllegalArgumentException if the group does not exist or the acting user is not the
   *                                  OWNER
   * @since 1.19.0
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
   * @since 1.19.0
   */
  List<GroupMember> listMembers(String groupId, String viewerId);
}