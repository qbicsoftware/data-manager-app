package life.qbic.usergroups.application.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.usergroups.api.GroupInfo;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupType;
import life.qbic.usergroups.api.MyGroupMembership;
import life.qbic.usergroups.application.GroupInfoProjection;
import life.qbic.usergroups.application.GroupMembershipProjection;
import life.qbic.usergroups.application.GroupService;

/**
 * <b>Group information service</b>
 *
 * <p>Implementation of the {@link GroupInformationService} API facade of the user groups context.
 * Delegates to the application-layer {@link GroupService} and maps the internal projections to
 * self-contained API DTOs — mirroring how {@code BasicUserInformationService} implements the
 * identity context's {@code UserInformationService}.</p>
 *
 * <p><b>Visibility:</b> the public directory and group lookups expose identity, name, description
 * and type only. Membership data is only ever returned for the requesting user themselves (my
 * groups).</p>
 *
 * @since 1.17.0
 */
public class GroupInformationServiceImpl implements GroupInformationService {

  private final GroupService groupService;

  public GroupInformationServiceImpl(GroupService groupService) {
    this.groupService = Objects.requireNonNull(groupService);
  }

  @Override
  public Optional<GroupInfo> findGroupById(String groupId) {
    return groupService.findGroupById(groupId).map(this::toGroupInfo);
  }

  @Override
  public List<GroupInfo> listPublicDirectory() {
    return groupService.listPublicDirectory().stream().map(this::toGroupInfo).toList();
  }

  @Override
  public List<MyGroupMembership> listMyGroups(String userId) {
    return groupService.listMyGroups(userId).stream().map(this::toMyGroupMembership).toList();
  }

  @Override
  public boolean isGroupNameAvailable(String name) {
    return groupService.isGroupNameAvailable(name);
  }

  private GroupInfo toGroupInfo(GroupInfoProjection projection) {
    GroupType apiType = switch (projection.groupType()) {
      case ORG -> GroupType.ORG;
      case ADHOC -> GroupType.ADHOC;
    };
    return new GroupInfo(
        projection.groupId().get(),
        projection.groupName().value(),
        projection.groupDescription().value().orElse(null),
        apiType);
  }

  private MyGroupMembership toMyGroupMembership(GroupMembershipProjection projection) {
    GroupType apiType = switch (projection.groupType()) {
      case ORG -> GroupType.ORG;
      case ADHOC -> GroupType.ADHOC;
    };
    life.qbic.usergroups.api.GroupRole apiRole = switch (projection.myRole()) {
      case OWNER -> life.qbic.usergroups.api.GroupRole.OWNER;
      case MANAGER -> life.qbic.usergroups.api.GroupRole.MANAGER;
      case MEMBER -> life.qbic.usergroups.api.GroupRole.MEMBER;
    };
    return new MyGroupMembership(
        projection.groupId().get(),
        projection.groupName().value(),
        projection.groupDescription().value().orElse(null),
        apiType,
        apiRole);
  }
}