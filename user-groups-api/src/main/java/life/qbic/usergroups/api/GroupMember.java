package life.qbic.usergroups.api;

import java.io.Serializable;

/**
 * <b>Group member DTO</b>
 *
 * <p>Projection of one member of a group for the management surface. Exposes only the member's
 * user id and their role inside the group; display names are resolved by the caller through the
 * identity context ({@code UserInformationService}).</p>
 *
 * @param userId the user id of the member
 * @param role   the member's role inside the group (OWNER / MANAGER / MEMBER)
 * @since 1.20.0
 */
public record GroupMember(String userId, GroupRole role) implements Serializable {

}