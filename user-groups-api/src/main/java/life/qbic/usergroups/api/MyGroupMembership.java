package life.qbic.usergroups.api;

import java.io.Serializable;

/**
 * <b>My group membership DTO</b>
 *
 * <p>Projection of one of the caller's group memberships for the "My Groups" UI. Carries
 * everything the UI needs <em>including</em> the caller's role inside the group.</p>
 *
 * @param groupId          the group id (stable)
 * @param groupName        the group's display name
 * @param groupDescription the group's description, may be {@code null} if none was set
 * @param groupType        the group type (ORG / ADHOC)
 * @param myRole           the caller's role inside the group (OWNER / MANAGER / MEMBER)
 * @param memberCount      the total number of members of the group (the caller is a member, so
 *                         the roster size is visible to them)
 * @since 1.19.0
 */
public record MyGroupMembership(String groupId, String groupName, String groupDescription,
                                GroupType groupType, GroupRole myRole, int memberCount)
    implements Serializable {

}