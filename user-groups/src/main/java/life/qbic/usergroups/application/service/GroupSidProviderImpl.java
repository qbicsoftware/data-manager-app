package life.qbic.usergroups.application.service;

import java.util.List;
import java.util.Objects;
import life.qbic.usergroups.api.GroupSidProvider;
import life.qbic.usergroups.application.GroupMembershipProjection;
import life.qbic.usergroups.application.GroupService;

/**
 * <b>Group SID provider</b>
 *
 * <p>Implementation of the {@link GroupSidProvider} API facade. Resolves a user's group sids from
 * their <em>active</em> group memberships only — dissolved groups never produce a sid.</p>
 *
 * <p>The returned strings follow the reserved {@code GROUP_<groupId>} prefix convention keyed on
 * the stable group id (never the display name), so persisted ACEs stay valid across group
 * renames.</p>
 *
 * @since 1.19.0
 */
public class GroupSidProviderImpl implements GroupSidProvider {

  private final GroupService groupService;

  public GroupSidProviderImpl(GroupService groupService) {
    this.groupService = Objects.requireNonNull(groupService);
  }

  @Override
  public List<String> listGroupSidsForUser(String userId) {
    return groupService.listMyGroups(userId).stream()
        .map(GroupMembershipProjection::groupId)
        .map(groupId -> GROUP_SID_PREFIX + groupId.get())
        .toList();
  }
}