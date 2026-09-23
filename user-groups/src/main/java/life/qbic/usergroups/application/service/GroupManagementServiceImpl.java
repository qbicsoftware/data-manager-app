package life.qbic.usergroups.application.service;

import java.util.List;
import java.util.Objects;
import life.qbic.usergroups.api.GroupManagementService;
import life.qbic.usergroups.api.GroupMember;
import life.qbic.usergroups.application.GroupMemberProjection;
import life.qbic.usergroups.application.GroupService;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupName;

/**
 * <b>Group management service implementation</b>
 *
 * <p>Implementation of the {@link GroupManagementService} API facade of the user groups context.
 * Delegates to the application-layer {@link GroupService} and maps {@code Result} values/errors
 * to the facade's contract (successful no-op, or a descriptive {@link IllegalArgumentException}).</p>
 *
 * <p>Role gates are enforced by the application service ({@link GroupService} + domain layer):
 * this facade deliberately contains no policy of its own.</p>
 *
 * @since 1.20.0
 */
public class GroupManagementServiceImpl implements GroupManagementService {

  private final GroupService groupService;

  public GroupManagementServiceImpl(GroupService groupService) {
    this.groupService = Objects.requireNonNull(groupService);
  }

  @Override
  public void addMember(String groupId, String actingUserId, String userId) {
    run(groupService.addMember(groupId, actingUserId, userId));
  }

  @Override
  public void removeMember(String groupId, String actingUserId, String userId) {
    run(groupService.removeMember(groupId, actingUserId, userId));
  }

  @Override
  public void appointManager(String groupId, String actingUserId, String userId) {
    run(groupService.appointManager(groupId, actingUserId, userId));
  }

  @Override
  public void demoteManager(String groupId, String actingUserId, String userId) {
    run(groupService.demoteManager(groupId, actingUserId, userId));
  }

  @Override
  public void renameGroup(String groupId, String actingUserId, String newName) {
    run(groupService.renameGroup(groupId, actingUserId, GroupName.from(newName)));
  }

  @Override
  public void updateDescription(String groupId, String actingUserId, String newDescription) {
    run(groupService.updateDescription(groupId, actingUserId,
        GroupDescription.from(newDescription)));
  }

  @Override
  public void dissolveGroup(String groupId, String actingUserId) {
    run(groupService.dissolveGroup(groupId, actingUserId));
  }

  @Override
  public List<GroupMember> listMembers(String groupId, String viewerId) {
    return groupService.listMembers(groupId, viewerId).stream()
        .map(GroupManagementServiceImpl::toApiMember)
        .toList();
  }

  private static GroupMember toApiMember(GroupMemberProjection projection) {
    life.qbic.usergroups.api.GroupRole apiRole = switch (projection.role()) {
      case OWNER -> life.qbic.usergroups.api.GroupRole.OWNER;
      case MANAGER -> life.qbic.usergroups.api.GroupRole.MANAGER;
      case MEMBER -> life.qbic.usergroups.api.GroupRole.MEMBER;
    };
    return new GroupMember(projection.userId(), apiRole);
  }

  private static void run(
      life.qbic.application.commons.Result<Void, life.qbic.application.commons.ApplicationException> result) {
    result.onError(error -> {
      throw new IllegalArgumentException(error.getMessage(), error);
    });
  }
}