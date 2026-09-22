package life.qbic.usergroups.application;

import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupType;

/**
 * <b>Group info projection</b>
 *
 * <p>Public-directory projection of a user group. By design exposes <b>only</b> the group's
 * identity, name, description and type — never membership information. See the visibility policy
 * of the user groups strategy (members are never exposed to non-members).</p>
 *
 * @param groupId      the group id
 * @param groupName    the group name
 * @param groupDescription the group description (may be empty)
 * @param groupType    the group type (ORG / ADHOC)
 * @since 1.17.0
 */
public record GroupInfoProjection(GroupId groupId, GroupName groupName,
                                  GroupDescription groupDescription, GroupType groupType) {

}