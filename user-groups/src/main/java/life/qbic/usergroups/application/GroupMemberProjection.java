package life.qbic.usergroups.application;

import life.qbic.usergroups.domain.model.GroupRole;

/**
 * <b>Group membership (member view) projection</b>
 *
 * <p>Projection of one member of a group, used to render the member roster in the management
 * surface. Exposes the member's user id and their role inside the group.</p>
 *
 * @param userId the user id of the member
 * @param role   the member's role inside the group (OWNER / MANAGER / MEMBER)
 * @since 1.20.0
 */
public record GroupMemberProjection(String userId, GroupRole role) {

}