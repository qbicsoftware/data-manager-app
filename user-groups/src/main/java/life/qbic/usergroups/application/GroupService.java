package life.qbic.usergroups.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupMembership;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupRole;
import life.qbic.usergroups.domain.model.UserGroup;
import life.qbic.usergroups.domain.registry.DomainRegistry;
import life.qbic.usergroups.domain.repository.GroupRepository;
import life.qbic.usergroups.domain.service.GroupDomainService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Group application service</b>
 *
 * <p>Facade for the user groups context that orchestrates the domain layer
 * ({@link GroupDomainService}) and the persistence port ({@link GroupRepository}) for the
 * application's use-cases.</p>
 *
 * <p>Responsibilities within the scope of story FEAT-USER-GROUPS-03 (create ad-hoc groups
 * self-service):</p>
 * <ul>
 *   <li>create an ad-hoc group for an authenticated user (the caller becomes OWNER)</li>
 *   <li>enforce the case-insensitive unique-name invariant and translate race condition
 *   violations into a user-friendly error</li>
 *   <li>list the caller's groups ("my groups")</li>
 *   <li>list the public directory (identity + name + description + type only, never members)</li>
 *   <li>remove a membership (auto-dissolve of empty ad-hoc groups is handled in the domain
 *   layer)</li>
 * </ul>
 *
 * @since 1.19.0
 */
public class GroupService {

  /**
   * User-facing error message when creating a group with a name that already exists
   * (case-insensitive). This is the exact wording covered by acceptance criterion (b).
   */
  static final String DUPLICATE_NAME_MESSAGE =
      "A group with the name '%s' already exists. Group names must be unique (case-insensitive).";

  /**
   * User-facing error message when a group with the requested id cannot be found.
   */
  static final String GROUP_NOT_FOUND_MESSAGE = "Group %s not found.";

  private final GroupRepository groupRepository;

  public GroupService(GroupRepository groupRepository) {
    this.groupRepository = groupRepository;
  }

  /**
   * Creates a new ad-hoc user group with the provided creator as its OWNER.
   *
   * <p>Performs a case-insensitive duplicate-name pre-check against the storage port. In case
   * of a race condition (two concurrent creations with the same name), the database unique index
   * rejects the second write and the resulting {@link DataIntegrityViolationException} is mapped
   * to the same user-friendly error — the raw exception is never leaked to the caller.</p>
   *
   * <p>This method is transactional: the duplicate check, the store, the reload and the
   * projection mapping all run inside a single transaction so no lazy collection of a detached
   * entity is ever touched outside a transaction.</p>
   *
   * @param creatorUserId the user id of the authenticated creator; must not be blank
   * @param name          the desired group name (validated by {@link GroupName})
   * @param description   the desired group description (may be empty)
   * @return a result wrapping the created {@link GroupInfoProjection}, or an error if the name is
   * already taken (case-insensitive) or the creator id is invalid
   * @since 1.19.0
   */
  @Transactional
  public Result<GroupInfoProjection, ApplicationException> createAdHocGroup(
      String creatorUserId, GroupName name, GroupDescription description) {
    if (creatorUserId == null || creatorUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid creator user id: " + creatorUserId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }

    Optional<UserGroup> existing = groupRepository.findByNameIgnoreCase(name);
    if (existing.isPresent()) {
      return duplicateNameError(name.value());
    }

    GroupId groupId = GroupId.create();
    Instant createdAt = Instant.now();
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(new ApplicationException(
          "Group creation failed.", ErrorCode.SERVICE_FAILED, ErrorParameters.empty()));
    }

    try {
      domainService.get().createAdHocGroup(groupId, name, description, creatorUserId, createdAt);
    } catch (RuntimeException e) {
      if (isDataIntegrityViolation(e)) {
        return duplicateNameError(name.value());
      }
      throw e;
    }

    Optional<UserGroup> created = groupRepository.findById(groupId);
    return created.<Result<GroupInfoProjection, ApplicationException>>map(
            group -> Result.fromValue(toInfoProjection(group)))
        .orElseGet(() -> Result.fromError(new ApplicationException(
            "Group creation failed.", ErrorCode.SERVICE_FAILED, ErrorParameters.empty())));
  }

  /**
   * Returns the caller's active group memberships ("my groups").
   *
   * <p>Only groups with status {@code ACTIVE} are returned; dissolved groups are excluded.
   *
   * @param userId the user id to list memberships for
   * @return the caller's active group memberships, including the caller's role inside each group
   * @since 1.19.0
   */
  @Transactional(readOnly = true)
  public List<GroupMembershipProjection> listMyGroups(String userId) {
    if (userId == null || userId.isBlank()) {
      return List.of();
    }
    List<UserGroup> groups = groupRepository.findActiveGroupsByUserId(userId);
    return groups.stream().map(group -> toMembershipProjection(group, userId)).toList();
  }

  /**
   * Returns all active groups for the public directory.
   *
   * <p>Exposes group identity, name, description and type <b>only</b> — never the member list
   * (visibility policy: group members are not exposed to non-members).
   *
   * @return a list of group info projections of all active groups
   * @since 1.19.0
   */
  @Transactional(readOnly = true)
  public List<GroupInfoProjection> listPublicDirectory() {
    return groupRepository.findAllActive().stream().map(this::toInfoProjection).toList();
  }

  /**
   * Looks up a single group by its id.
   *
   * <p>Only active groups are returned; dissolved groups resolve to an empty result so the
   * public directory and API facade never surface soft-dissolved groups.</p>
   *
   * @param groupId the group id to look up
   * @return a group info projection if the group exists and is active, else empty
   * @since 1.19.0
   */
  @Transactional(readOnly = true)
  public Optional<GroupInfoProjection> findGroupById(String groupId) {
    GroupId parsedId;
    try {
      parsedId = GroupId.from(groupId);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
    return groupRepository.findById(parsedId)
        .filter(UserGroup::isActive)
        .map(this::toInfoProjection);
  }

  /**
   * Queries whether a desired group name is still available (case-insensitive).
   *
   * <p>Delegates to the storage port's case-insensitive name lookup. The actual uniqueness is
   * backed by the database unique index (utf8mb4_unicode_ci); this method only serves the
   * availability check of the UI/API layer.</p>
   *
   * @param name the desired group name
   * @return {@code true} if the name is not in use by any group, {@code false} otherwise
   * @since 1.19.0
   */
  @Transactional(readOnly = true)
  public boolean isGroupNameAvailable(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    GroupName groupName;
    try {
      groupName = GroupName.from(name);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return groupRepository.findByNameIgnoreCase(groupName).isEmpty();
  }

  /**
   * Removes a user's membership from a group.
   *
   * <p>If the group is an ad-hoc group and the roster becomes empty, the domain layer dissolves
   * the group (soft dissolve) and dispatches a {@code GroupDissolved} event.</p>
   *
   * @param groupId the id of the group
   * @param userId  the id of the user to remove
   * @return a {@link Result} with no value on success, or an error if the group does not exist or
   * the user is not a member
   * @since 1.19.0
   */
  @Transactional
  public Result<Void, ApplicationException> removeMembership(String groupId, String userId) {
    GroupId parsedId;
    try {
      parsedId = GroupId.from(groupId);
    } catch (IllegalArgumentException e) {
      return Result.fromError(new ApplicationException(
          String.format(GROUP_NOT_FOUND_MESSAGE, groupId), ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }

    Optional<UserGroup> group = groupRepository.findById(parsedId);
    if (group.isEmpty()) {
      return Result.fromError(new ApplicationException(
          String.format(GROUP_NOT_FOUND_MESSAGE, groupId), ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }

    boolean isMember = group.get().memberships().stream()
        .anyMatch(m -> m.userId().equals(userId));
    if (!isMember) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is not a member of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }

    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(new ApplicationException(
          "Group operation failed.", ErrorCode.SERVICE_FAILED, ErrorParameters.empty()));
    }

    Optional<UserGroup> updated = domainService.get().removeMembership(parsedId, userId);
    if (updated.isEmpty()) {
      // membership was legitimately checked above; this only happens on a concurrent removal
      return Result.fromError(new ApplicationException(
          "User " + userId + " is not a member of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    return Result.fromValue(null);
  }

  /**
   * Adds a regular member to an ad-hoc group.
   *
   * <p>Role-gated: the caller must hold role OWNER or MANAGER inside the group (enforced by the
   * domain layer). The target user must not be a member yet.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER or MANAGER)
   * @param userId       the user to add as a regular member
   * @return a {@link Result} with no value on success, or an error if the group does not exist,
   * the user to add is already a member, or the acting user lacks the required role
   * @since 1.20.0
   */
  @Transactional
  public Result<Void, ApplicationException> addMember(String groupId, String actingUserId,
      String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.memberships().stream().anyMatch(m -> m.userId().equals(userId))) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is already a member of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    Optional<UserGroup> updated = domainService.get().addMember(group.id(), actingUserId, userId,
        Instant.now());
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Removes a regular member from an ad-hoc group.
   *
   * <p>Role-gated: the caller must hold role OWNER or MANAGER (a manager may not remove another
   * manager, and nobody may remove the owner). If the removal empties the group, it is
   * auto-dissolved.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the removal
   * @param userId       the member to remove
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.20.0
   */
  @Transactional
  public Result<Void, ApplicationException> removeMember(String groupId, String actingUserId,
      String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    Optional<UserGroup> updated = domainService.get().removeMember(maybeGroup.get().id(),
        actingUserId, userId);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Appoints a regular member as a manager of an ad-hoc group.
   *
   * <p>Role-gated: only the OWNER may appoint managers.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @param userId       the member to promote to MANAGER
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.20.0
   */
  @Transactional
  public Result<Void, ApplicationException> appointManager(String groupId, String actingUserId,
      String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    Optional<UserGroup> updated = domainService.get().appointManager(maybeGroup.get().id(),
        actingUserId, userId);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Demotes a manager back to a regular member of an ad-hoc group.
   *
   * <p>Role-gated: only the OWNER may demote managers.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @param userId       the manager to demote
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.20.0
   */
  @Transactional
  public Result<Void, ApplicationException> demoteManager(String groupId, String actingUserId,
      String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    Optional<UserGroup> updated = domainService.get().demoteManager(maybeGroup.get().id(),
        actingUserId, userId);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Renames an ad-hoc group.
   *
   * <p>Role-gated: the OWNER and MANAGER may rename. The new name must be unique
   * (case-insensitive); a violation is rejected with {@link ErrorCode#DUPLICATE_GROUP_NAME}.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER or MANAGER)
   * @param newName      the new group name
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted or the new name is already taken
   * @since 1.20.0
   */
  @Transactional
  public Result<Void, ApplicationException> renameGroup(String groupId, String actingUserId,
      GroupName newName) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    Optional<UserGroup> existing = groupRepository.findByNameIgnoreCase(newName);
    if (existing.isPresent() && !existing.get().id().equals(maybeGroup.get().id())) {
      return Result.fromError(duplicateNameApplicationError(newName.value()));
    }
    Optional<UserGroup> updated = domainService.get().renameGroup(maybeGroup.get().id(),
        actingUserId, newName);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Updates the description of an ad-hoc group.
   *
   * <p>Role-gated: the OWNER and MANAGER may change the description.</p>
   *
   * @param groupId        the id of the group
   * @param actingUserId   the user performing the operation (must hold OWNER or MANAGER)
   * @param newDescription the new group description
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.20.0
   */
  @Transactional
  public Result<Void, ApplicationException> updateDescription(String groupId, String actingUserId,
      GroupDescription newDescription) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    Optional<UserGroup> updated = domainService.get().updateDescription(maybeGroup.get().id(),
        actingUserId, newDescription);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Dissolves an ad-hoc group by its owner (explicit dissolve).
   *
   * <p>Role-gated: only the OWNER may dissolve explicitly.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER)
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.20.0
   */
  @Transactional
  public Result<Void, ApplicationException> dissolveGroup(String groupId, String actingUserId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    Optional<UserGroup> updated = domainService.get().dissolve(maybeGroup.get().id(),
        actingUserId);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Returns the members of the given active group.
   *
   * <p><b>Visibility:</b> members of a group are only exposed to the group's own members (and
   * the QBiC admin for oversight); non-members receive an empty result. This enforces the
   * strategy's visibility rule in the application layer.</p>
   *
   * @param groupId  the id of the group
   * @param viewerId the user requesting the list (must be a member of the group)
   * @return the group's members, or an empty list if the group does not exist, is inactive, or
   * the viewer is not a member
   * @since 1.20.0
   */
  @Transactional(readOnly = true)
  public List<GroupMemberProjection> listMembers(String groupId, String viewerId) {
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return List.of();
    }
    UserGroup group = maybeGroup.get();
    boolean isMember = group.memberships().stream().anyMatch(m -> m.userId().equals(viewerId));
    if (!isMember) {
      return List.of();
    }
    return group.memberships().stream()
        .map(m -> new GroupMemberProjection(m.userId(), m.role()))
        .toList();
  }

  private Optional<UserGroup> resolveActiveGroup(String groupId) {
    GroupId parsedId;
    try {
      parsedId = GroupId.from(groupId);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
    return groupRepository.findById(parsedId).filter(UserGroup::isActive);
  }

  private ApplicationException groupNotFound(String groupId) {
    return new ApplicationException(String.format(GROUP_NOT_FOUND_MESSAGE, groupId),
        ErrorCode.GENERAL, ErrorParameters.empty());
  }

  private static ApplicationException accessDenied(String userId, String groupId) {
    return new ApplicationException("User " + userId + " is not allowed to perform this "
        + "operation on group " + groupId, ErrorCode.ACCESS_DENIED, ErrorParameters.empty());
  }

  private static ApplicationException systemFailure() {
    return new ApplicationException("Group operation failed.", ErrorCode.SERVICE_FAILED,
        ErrorParameters.empty());
  }

  private Result<GroupInfoProjection, ApplicationException> duplicateNameError(String name) {
    return Result.fromError(duplicateNameApplicationError(name));
  }

  private static ApplicationException duplicateNameApplicationError(String name) {
    return new ApplicationException(
        String.format(DUPLICATE_NAME_MESSAGE, name), ErrorCode.DUPLICATE_GROUP_NAME,
        ErrorParameters.of(name));
  }

  private boolean isDataIntegrityViolation(RuntimeException e) {
    Throwable cause = e;
    while (cause != null) {
      if (cause instanceof DataIntegrityViolationException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private GroupMembershipProjection toMembershipProjection(UserGroup group, String userId) {
    GroupRole role = group.memberships().stream()
        .filter(m -> m.userId().equals(userId))
        .map(GroupMembership::role)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "User " + userId + " has no membership in group " + group.id()));
    return new GroupMembershipProjection(group.id(), group.name(), group.description(),
        group.type(), role);
  }

  private GroupInfoProjection toInfoProjection(UserGroup group) {
    return new GroupInfoProjection(group.id(), group.name(), group.description(), group.type());
  }
}