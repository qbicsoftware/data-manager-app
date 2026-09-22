package life.qbic.usergroups.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.usergroups.domain.event.GroupCreated;
import life.qbic.usergroups.domain.event.GroupDissolved;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.UserGroup;
import life.qbic.usergroups.domain.repository.GroupRepository;

/**
 * <b>Group Domain Service</b>
 *
 * <p>Domain service within the user groups context. Takes over the ad-hoc group creation and
 * membership removal, and publishes domain events once the corresponding domain state changes have
 * been persisted.</p>
 *
 * <p>Mirrors the identity context's {@code UserDomainService}:
 * create → store via repository → dispatch {@link GroupCreated};
 * removal of the last membership → dissolve (aggregate) → dispatch {@link GroupDissolved}.</p>
 *
 * @since 1.19.0
 */
public class GroupDomainService {

  private final GroupRepository groupRepository;

  public GroupDomainService(GroupRepository groupRepository) {
    this.groupRepository = groupRepository;
  }

  /**
   * Creates a new ad-hoc user group with the creator as its OWNER.
   *
   * <p>Note: this will create a new domain event of type {@link GroupCreated}. The caller is
   * responsible for the duplicate-name check (case-insensitive) before invoking this method; this
   * domain service does not enforce cross-aggregate uniqueness.</p>
   *
   * @param id            the id of the new group
   * @param name          the (unique, case-insensitive) group name
   * @param description   the group description (optional)
   * @param creatorUserId the user id of the creating user
   * @param createdAt     the creation timestamp
   * @since 1.19.0
   */
  public void createAdHocGroup(GroupId id, GroupName name, GroupDescription description,
      String creatorUserId, Instant createdAt) {
    var group = UserGroup.createAdHoc(id, name, description, creatorUserId, createdAt);
    groupRepository.store(group);
    var groupCreatedEvent = GroupCreated.create(group.id().get(), group.name().value(),
        group.type(), group.createdBy());
    DomainEventDispatcher.instance().dispatch(groupCreatedEvent);
  }

  /**
   * Removes a user's membership from a group.
   *
   * <p>If the group is an ad-hoc group and the roster becomes empty as a result, the group is
   * dissolved (soft dissolve via the aggregate) and a {@link GroupDissolved} event is dispatched.</p>
   *
   * <p>If the group does not exist, or the user is not a member, this method does nothing and
   * returns an empty {@link Optional}. Callers that need to distinguish these cases must resolve
   * the group via {@link #findGroup(GroupId)} first.</p>
   *
   * @param groupId the id of the group
   * @param userId  the id of the user to remove
   * @return the updated group if a membership was removed, otherwise an empty {@link Optional}
   * @since 1.19.0
   */
  public Optional<UserGroup> removeMembership(GroupId groupId, String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    boolean isMember = group.memberships().stream().anyMatch(m -> m.userId().equals(userId));
    if (!isMember) {
      return Optional.empty();
    }
    boolean dissolved = group.removeMembership(userId);
    groupRepository.store(group);
    if (dissolved) {
      var dissolvedEvent = GroupDissolved.create(group.id().get(), group.name().value(),
          group.type(), userId);
      DomainEventDispatcher.instance().dispatch(dissolvedEvent);
    }
    return Optional.of(group);
  }

  /**
   * Finds a group by its id.
   *
   * @param groupId the group id
   * @return the group, or an empty {@link Optional} if no group with this id exists
   * @since 1.19.0
   */
  public Optional<UserGroup> findGroup(GroupId groupId) {
    return groupRepository.findById(groupId);
  }

  /**
   * Lists all active groups the given user is a member of ("my groups").
   *
   * @param userId the user id
   * @return list of active groups of the user
   * @since 1.19.0
   */
  public List<UserGroup> listMyGroups(String userId) {
    return groupRepository.findActiveGroupsByUserId(userId);
  }

  /**
   * Lists all active groups (public directory). Exposes group identity, name, description and
   * type only; never membership information.
   *
   * @return list of all active groups
   * @since 1.19.0
   */
  public List<UserGroup> listPublicDirectory() {
    return groupRepository.findAllActive();
  }
}