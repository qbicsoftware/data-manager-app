package life.qbic.usergroups.application;

import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupRole;
import life.qbic.usergroups.domain.model.GroupType;

/**
 * <b>Group membership projection</b>
 *
 * <p>Application-layer projection of one of the caller's group memberships. Carries everything the
 * "My Groups" UI needs <em>including</em> the caller's role inside the group.
 *
 * @param groupId        the group id
 * @param groupName      the group name
 * @param groupDescription the group description (may be empty)
 * @param groupType      the group type (ORG / ADHOC)
 * @param myRole         the caller's role inside the group
 * @since 1.19.0
 */
public record GroupMembershipProjection(GroupId groupId, GroupName groupName,
                                        GroupDescription groupDescription, GroupType groupType,
                                        GroupRole myRole) {

}