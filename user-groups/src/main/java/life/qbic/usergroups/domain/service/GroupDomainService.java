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
import life.qbic.usergroups.domain.model.GroupType;
import life.qbic.usergroups.domain.model.UserGroup;
import life.qbic.usergroups.domain.repository.GroupRepository;

/**
 * <b>Group Domain Service</b>
 *
 * <p>Domain service within the user groups context. Publishes the domain events once the
 * corresponding domain state changes have been persisted, and bridges the aggregate's
 * type-agnostic primitives to the use-cases of the application layer.</p>
 *
 * <p>The aggregate is type-agnostic: the org and ad-hoc entry points below persist the same
 * primitives. <em>Authorization</em> (who may act) is enforced upstream in the application layer
 * (system-admin gate for org groups, OWNER/MANAGER membership gates for ad-hoc groups), never
 * here.</p>
 *
 * <p>Dissolution is always explicit: {@link GroupDissolved} is dispatched only by the explicit
 * dissolve paths (ad-hoc owner dissolve, admin org dissolve). Removing a member never dissolves a
 * group.</p>
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
   * Creates a new organisational user group (empty roster, no OWNER row).
   *
   * @param id            the id of the new group
   * @param name          the (unique, case-insensitive) group name
   * @param description   the group description (optional)
   * @param createdByUserId the user id of the creating QBiC administrator
   * @param createdAt     the creation timestamp
   * @since 1.19.0
   */
  public void createOrgGroup(GroupId id, GroupName name, GroupDescription description,
      String createdByUserId, Instant createdAt) {
    var group = UserGroup.createOrg(id, name, description, createdByUserId, createdAt);
    groupRepository.store(group);
    var groupCreatedEvent = GroupCreated.create(group.id().get(), group.name().value(),
        group.type(), group.createdBy());
    DomainEventDispatcher.instance().dispatch(groupCreatedEvent);
  }

  /**
   * Removes a user's membership from a group (self-removal; non-owner).
   *
   * <p>The group is never dissolved by this operation. A {@link MemberRemovedFromGroup} event is
   * dispatched when a membership was actually removed.</p>
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
    boolean removed = group.removeMembership(userId);
    groupRepository.store(group);
    if (removed) {
      DomainEventDispatcher.instance().dispatch(
          MemberRemovedFromGroup.create(group.id().get(), userId, userId));
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
   * Adds a regular member to a group.
   *
   * <p>No authorization here (application layer enforced). After a successful persist a
   * {@link MemberAddedToGroup} event is dispatched.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (authorization already enforced)
   * @param userId       the user to add as a regular member
   * @param joinedAt     the join timestamp
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected (group dissolved or the target is already a member)
   * @since 1.19.0
   */
  public Optional<UserGroup> addMember(GroupId groupId, String actingUserId, String userId,
      Instant joinedAt) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    try {
      group.addMember(userId, joinedAt);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(
        MemberAddedToGroup.create(group.id().get(), userId, actingUserId));
    return Optional.of(group);
  }

  /**
   * Removes another member's membership from a group.
   *
   * <p>No authorization here (application layer enforced). The group is never dissolved by this
   * operation; a {@link MemberRemovedFromGroup} event is dispatched when a member was actually
   * removed.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the removal (authorization already enforced)
   * @param userId       the member to remove
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.19.0
   */
  public Optional<UserGroup> removeMember(GroupId groupId, String actingUserId, String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    try {
      boolean removed = group.removeMember(actingUserId, userId);
      if (!removed) {
        return Optional.empty();
      }
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(
        MemberRemovedFromGroup.create(group.id().get(), userId, actingUserId));
    return Optional.of(group);
  }

  /**
   * Appoints a regular member as a MANAGER.
   *
   * <p>No authorization here (application layer enforced). On success a
   * {@link GroupMembershipRoleChanged} event (MEMBER → MANAGER) is dispatched. A user who is
   * already a MANAGER is a no-op (no event, retrieved as-is).</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (authorization already enforced)
   * @param userId       the member to promote to MANAGER
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.19.0
   */
  public Optional<UserGroup> appointManager(GroupId groupId, String actingUserId, String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    boolean changed;
    try {
      changed = group.setRoleOf(userId, GroupRole.MANAGER);
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
   * Demotes a MANAGER back to a regular MEMBER.
   *
   * <p>No authorization here (application layer enforced). On success a
   * {@link GroupMembershipRoleChanged} event (MANAGER → MEMBER) is dispatched.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (authorization already enforced)
   * @param userId       the manager to demote
   * @return the updated group, or an empty {@link Optional} if the group does not exist or the
   * operation was rejected
   * @since 1.19.0
   */
  public Optional<UserGroup> demoteManager(GroupId groupId, String actingUserId, String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    boolean changed;
    try {
      changed = group.setRoleOf(userId, GroupRole.MEMBER);
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
   * Appoints a user as a MANAGER of an <b>org</b> group (admin-governed).
   *
   * <p>Direct appointment of a non-member dispatches a {@link MemberAddedToGroup}; promoting an
   * existing MEMBER dispatches {@link GroupMembershipRoleChanged}(MEMBER → MANAGER). A user who
   * is already a MANAGER is a no-op.</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @param userId        the user to appoint (non-member or regular MEMBER)
   * @param joinedAt      the join timestamp for direct appointment
   * @return the updated group, or an empty {@link Optional} if the group does not exist, is
   * dissolved, or the target user id is blank
   * @since 1.19.0
   */
  public Optional<UserGroup> appointOrgManager(GroupId groupId, String actingAdminUserId,
      String userId, Instant joinedAt) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    if (group.status() == GroupStatus.DISSOLVED) {
      return Optional.empty();
    }
    boolean wasMember = group.memberships().stream()
        .anyMatch(m -> m.userId().equals(userId));
    if (wasMember) {
      // existing member → role promotion
      Optional<UserGroup> promoted = appointManager(groupId, actingAdminUserId, userId);
      return promoted.isEmpty() ? Optional.empty() : Optional.of(promoted.get());
    }
    // direct appointment of a non-member: a MANAGER membership is created with MemberAdded event
    try {
      group.addMember(userId, joinedAt);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    boolean changed;
    try {
      changed = group.setRoleOf(userId, GroupRole.MANAGER);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    if (!changed) {
      return Optional.of(group);
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(
        MemberAddedToGroup.create(group.id().get(), userId, actingAdminUserId));
    return Optional.of(group);
  }

  /**
   * Removes a member's (or manager's) membership from an <b>org</b> group (admin-governed).
   *
   * <p>A {@link MemberRemovedFromGroup} event is dispatched. The group is never dissolved by
   * this operation.</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @param userId        the manager (or member) to remove
   * @return the updated group, or an empty {@link Optional} if the group does not exist, is
   * dissolved, or the target is not a member
   * @since 1.19.0
   */
  public Optional<UserGroup> removeOrgManager(GroupId groupId, String actingAdminUserId,
      String userId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    if (group.status() == GroupStatus.DISSOLVED) {
      return Optional.empty();
    }
    try {
      boolean removed = group.removeMembership(userId);
      if (!removed) {
        return Optional.empty();
      }
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(
        MemberRemovedFromGroup.create(group.id().get(), userId, actingAdminUserId));
    return Optional.of(group);
  }

  /**
   * Adds a regular MEMBER to an <b>org</b> group (admin-governed).
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @param userId        the user to add as a regular member
   * @param joinedAt      the join timestamp
   * @return the updated group, or an empty {@link Optional} if the group does not exist, is
   * dissolved, or is already a member
   * @since 1.19.0
   */
  public Optional<UserGroup> addOrgMember(GroupId groupId, String actingAdminUserId,
      String userId, Instant joinedAt) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    if (group.status() == GroupStatus.DISSOLVED) {
      return Optional.empty();
    }
    boolean wasMember = group.memberships().stream()
        .anyMatch(m -> m.userId().equals(userId));
    if (wasMember) {
      return Optional.of(group);
    }
    try {
      group.addMember(userId, joinedAt);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(
        MemberAddedToGroup.create(group.id().get(), userId, actingAdminUserId));
    return Optional.of(group);
  }

  /**
   * Demotes a MANAGER back to a regular MEMBER of an <b>org</b> group (admin-governed).
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @param userId        the manager to demote
   * @return the updated group, or an empty {@link Optional} if the group does not exist, is
   * dissolved, or the target is not a manager
   * @since 1.19.0
   */
  public Optional<UserGroup> demoteOrgManager(GroupId groupId, String actingAdminUserId,
      String userId) {
    return demoteManager(groupId, actingAdminUserId, userId);
  }

  /**
   * Removes a member's membership from an <b>org</b> group (admin-governed).
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @param userId        the member to remove
   * @return the updated group, or an empty {@link Optional} if the group does not exist, is
   * dissolved, or the target is not a member
   * @since 1.19.0
   */
  public Optional<UserGroup> removeOrgMember(GroupId groupId, String actingAdminUserId,
      String userId) {
    return removeOrgManager(groupId, actingAdminUserId, userId);
  }

  /**
   * Renames an <b>org</b> group (admin-governed).
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @param newName       the new group name
   * @return the updated group, or an empty {@link Optional} if the group does not exist or is
   * dissolved
   * @since 1.19.0
   */
  public Optional<UserGroup> renameOrgGroup(GroupId groupId, String actingAdminUserId,
      GroupName newName) {
    return renameGroup(groupId, actingAdminUserId, newName);
  }

  /**
   * Updates an <b>org</b> group's description (admin-governed).
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @param newDescription the new group description
   * @return the updated group, or an empty {@link Optional} if the group does not exist or is
   * dissolved
   * @since 1.19.0
   */
  public Optional<UserGroup> updateOrgGroupDescription(GroupId groupId, String actingAdminUserId,
      GroupDescription newDescription) {
    return updateDescription(groupId, actingAdminUserId, newDescription);
  }

  /**
   * Dissolves an <b>org</b> group (admin-governed).
   *
   * <p>An explicit admin dissolve is the only way an org group ends (org groups never
   * auto-dissolve and never carry an OWNER row). On success a {@link GroupDissolved} event is
   * dispatched.</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user id of the acting QBiC administrator
   * @return the dissolved group, or an empty {@link Optional} if the group does not exist or is
   * already dissolved
   * @since 1.19.0
   */
  public Optional<UserGroup> dissolveOrgGroup(GroupId groupId, String actingAdminUserId) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    if (group.status() == GroupStatus.DISSOLVED) {
      return Optional.of(group);
    }
    group.dissolve();
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupDissolved.create(
        group.id().get(), group.name().value(), group.type(), actingAdminUserId));
    return Optional.of(group);
  }

  /**
   * Renames a group.
   *
   * <p>No authorization here (application layer enforced). On success a
   * {@link GroupProfileUpdated} event is dispatched.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (authorization already enforced)
   * @param newName      the new group name
   * @return the updated group, or an empty {@link Optional} if the group does not exist or is
   * dissolved
   * @since 1.19.0
   */
  public Optional<UserGroup> renameGroup(GroupId groupId, String actingUserId, GroupName newName) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    String oldName = group.name().value();
    try {
      group.rename(newName);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupProfileUpdated.create(
        group.id().get(), oldName, newName.value(), actingUserId));
    return Optional.of(group);
  }

  /**
   * Updates a group's description.
   *
   * <p>No authorization here (application layer enforced). On success a
   * {@link GroupProfileUpdated} event is dispatched.</p>
   *
   * @param groupId        the id of the group
   * @param actingUserId   the user performing the operation (authorization already enforced)
   * @param newDescription the new group description
   * @return the updated group, or an empty {@link Optional} if the group does not exist or is
   * dissolved
   * @since 1.19.0
   */
  public Optional<UserGroup> updateDescription(GroupId groupId, String actingUserId,
      GroupDescription newDescription) {
    Optional<UserGroup> maybeGroup = groupRepository.findById(groupId);
    if (maybeGroup.isEmpty()) {
      return Optional.empty();
    }
    UserGroup group = maybeGroup.get();
    try {
      group.updateDescription(newDescription);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Optional.empty();
    }
    groupRepository.store(group);
    DomainEventDispatcher.instance().dispatch(GroupProfileUpdated.create(
        group.id().get(), group.name().value(), group.name().value(), actingUserId));
    return Optional.of(group);
  }

  /**
   * Dissolves a group explicitly (ad-hoc owner dissolve or admin org dissolve).
   *
   * <p>This is the only path that dispatches {@link GroupDissolved}. No authorization here
   * (application layer enforced the caller is authorized to dissolve the group type).</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the dissolve (authorization already enforced)
   * @return the dissolved group, or an empty {@link Optional} if the group does not exist
   * @since 1.19.0
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
    group.dissolve();
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