package life.qbic.usergroups.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.usergroups.domain.event.GroupCreated;
import life.qbic.usergroups.domain.event.GroupDissolved;
import life.qbic.usergroups.domain.event.GroupMembershipRoleChanged;
import life.qbic.usergroups.domain.event.GroupProfileUpdated;
import life.qbic.usergroups.domain.event.MemberAddedToGroup;
import life.qbic.usergroups.domain.event.MemberRemovedFromGroup;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupRole;
import life.qbic.usergroups.domain.model.GroupStatus;
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
   * Adds a regular member to an ad-hoc group.
   *
   * <p>Role-gated by the aggregate ({@code GroupMember}: only the OWNER or a MANAGER may add
   * regular members). After a successful persist a {@link MemberAddedToGroup} event is dispatched
   * for the notification profile.</p>
   *
   * @param groupId        the id of the group
   * @param actingUserId   the user performing the operation (must hold OWNER or MANAGER)
   * @param userId         the user to add as a regular member
   * @param joinedAt       the join timestamp
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected (group dissolved, acting user lacks the required role, or the target
   * is already a member)
   * @since 1.20.0
   */
  public Optional<UserGroup> addMember(GroupId groupId, String actingUserId, String userId,
      Instant joinedAt) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    try {
      group.addMember(actingUserId, userId, joinedAt);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(
        MemberAddedToGroup.create(group.id().get(), userId, actingUserId));
    return Optional.of(group);
  }

  /**
   * Removes a regular member from an ad-hoc group.
   *
   * <p>Role-gated by the aggregate: only the OWNER or a MANAGER may remove other members. If the
   * removal empties the group the aggregate auto-dissolves it. A {@link MemberRemovedFromGroup}
   * event is dispatched when a member was actually removed.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the removal
   * @param userId       the member to remove
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.20.0
   */
  public Optional<UserGroup> removeMember(GroupId groupId, String actingUserId, String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    boolean dissolved;
    try {
      dissolved = group.removeMember(actingUserId, userId);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(
        MemberRemovedFromGroup.create(group.id().get(), userId, actingUserId));
    if (dissolved) {
      DomainEventDispatcher.instance().dispatch(GroupDissolved.create(
          group.id().get(), group.name().value(), group.type(), actingUserId));
    }
    return Optional.of(group);
  }

  /**
   * Appoints a regular member as a manager of an ad-hoc group.
   *
   * <p>Role-gated by the aggregate: only the OWNER may appoint managers. On success a
   * {@link GroupMembershipRoleChanged} event (audit hook; role changes are audit-log-only per
   * the notification profile) is dispatched.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @param userId       the member to promote to MANAGER
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.20.0
   */
  public Optional<UserGroup> appointManager(GroupId groupId, String actingUserId, String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    boolean changed;
    try {
      changed = group.appointManager(actingUserId, userId);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    if (!changed) {
      return Optional.of(group);
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupMembershipRoleChanged.create(
        group.id().get(), userId, GroupRole.MEMBER, GroupRole.MANAGER, actingUserId));
    return Optional.of(group);
  }

  /**
   * Demotes a manager back to a regular member of an ad-hoc group.
   *
   * <p>Role-gated by the aggregate: only the OWNER may demote managers. On success a
   * {@link GroupMembershipRoleChanged} event is dispatched.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @param userId       the manager to demote
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.20.0
   */
  public Optional<UserGroup> demoteManager(GroupId groupId, String actingUserId, String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    boolean changed;
    try {
      changed = group.demoteManager(actingUserId, userId);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    if (!changed) {
      return Optional.of(group);
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupMembershipRoleChanged.create(
        group.id().get(), userId, GroupRole.MANAGER, GroupRole.MEMBER, actingUserId));
    return Optional.of(group);
  }

  /**
   * Renames an ad-hoc group.
   *
   * <p>Role-gated by the aggregate: the OWNER and MANAGER may rename. The unique-name check is
   * the caller's responsibility (application layer). On success a {@link GroupProfileUpdated}
   * event is dispatched.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER or MANAGER)
   * @param newName      the new group name
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.20.0
   */
  public Optional<UserGroup> renameGroup(GroupId groupId, String actingUserId, GroupName newName) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    String oldName = group.name().value();
    try {
      group.rename(actingUserId, newName);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupProfileUpdated.create(
        group.id().get(), oldName, newName.value(), actingUserId));
    return Optional.of(group);
  }

  /**
   * Updates an ad-hoc group's description.
   *
   * <p>Role-gated by the aggregate: the OWNER and MANAGER may change the description. On success
   * a {@link GroupProfileUpdated} event is dispatched.</p>
   *
   * @param groupId        the id of the group
   * @param actingUserId   the user performing the operation (must hold OWNER or MANAGER)
   * @param newDescription the new group description
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.20.0
   */
  public Optional<UserGroup> updateDescription(GroupId groupId, String actingUserId,
      GroupDescription newDescription) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    try {
      group.updateDescription(actingUserId, newDescription);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupProfileUpdated.create(
        group.id().get(), group.name().value(), group.name().value(), actingUserId));
    return Optional.of(group);
  }

  /**
   * Dissolves an ad-hoc group by its owner (explicit dissolve).
   *
   * <p>Role-gated by the aggregate: only the OWNER may dissolve explicitly. The dissolve may also
   * happen implicitly when the last membership is removed (see {@link #removeMembership}). When a
   * group is dissolved a {@link GroupDissolved} event is dispatched.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @return the dissolved group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.20.0
   */
  public Optional<UserGroup> dissolve(GroupId groupId, String actingUserId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    if (group.status() == GroupStatus.DISSOLVED) {
      return Optional.of(group);
    }
    try {
      group.dissolveByOwner(actingUserId);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupDissolved.create(
        group.id().get(), group.name().value(), group.type(), actingUserId));
    return Optional.of(group);
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