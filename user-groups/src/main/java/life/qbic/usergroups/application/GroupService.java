package life.qbic.usergroups.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.identity.api.UserInformationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import life.qbic.usergroups.domain.model.GroupDescription;
import life.qbic.usergroups.domain.model.GroupId;
import life.qbic.usergroups.domain.model.GroupMembership;
import life.qbic.usergroups.domain.model.GroupName;
import life.qbic.usergroups.domain.model.GroupRole;
import life.qbic.usergroups.domain.model.GroupType;
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
  private final UserInformationService userInformationService;
  private final GroupAdministrationPermission groupAdministrationPermission;

  public GroupService(GroupRepository groupRepository,
      UserInformationService userInformationService) {
    this(groupRepository, userInformationService, userId -> false);
  }

  /**
   * Creates a new {@link GroupService} with an injected admin-gate port.
   *
   * @param groupRepository               the group storage port
   * @param userInformationService        the identity lookup port
   * @param groupAdministrationPermission the admin-gate port (org-group operations); never
   *                                      {@code null}
   * @since 1.21.0
   */
  public GroupService(GroupRepository groupRepository,
      UserInformationService userInformationService,
      GroupAdministrationPermission groupAdministrationPermission) {
    this.groupRepository = groupRepository;
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupAdministrationPermission = Objects.requireNonNull(groupAdministrationPermission,
        "groupAdministrationPermission must not be null");
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
   * Creates a new organisational user group by a QBiC administrator.
   *
   * <p>Admin gate (AC3): the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted.</p>
   *
   * <p>Reuses the exact duplicate-name flow of {@link #createAdHocGroup(String, GroupName,
   * GroupDescription)}: a case-insensitive pre-check plus a {@link DataIntegrityViolationException}
   * race mapping, both yielding the existing {@link ErrorCode#DUPLICATE_GROUP_NAME} message (AC2).
   * The uniqueness is global over all group types, matching the database unique index
   * {@code uk_user_group_name}.</p>
   *
   * <p>The created org group has an empty roster — org groups carry no OWNER membership row; the
   * QBiC admin acts as owner-equivalent at the application layer (D4).</p>
   *
   * @param actingAdminUserId the user id of the acting QBiC administrator; must not be blank
   * @param name              the desired group name (validated by {@link GroupName})
   * @param description       the desired group description (may be empty)
   * @return a result wrapping the created org-group {@link GroupInfoProjection}, or an error if
   * the caller is not an admin, the name is already taken (case-insensitive) or the admin id is
   * invalid
   * @since 1.21.0
   */
  @Transactional
  public Result<GroupInfoProjection, ApplicationException> createOrgGroup(String actingAdminUserId,
      GroupName name, GroupDescription description) {
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id: " + actingAdminUserId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(new ApplicationException(
          "User " + actingAdminUserId + " is not a QBiC administrator and is not allowed to "
              + "create organisational groups.", ErrorCode.ACCESS_DENIED, ErrorParameters.empty()));
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
      domainService.get().createOrgGroup(groupId, name, description, actingAdminUserId, createdAt);
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
   * Returns the member count of an organisational group (admin oversight).
   *
   * <p>Admin gate: the caller must be a QBiC administrator, and the target group must be an
   * active organisational group (the public directory never carries membership data; this is
   * the admin-only seam for it). The count is derived from the loaded aggregate's roster, with
   * the same semantics as the member-count badge in My Groups.</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user requesting the count; must be a QBiC administrator
   * @return the number of members in the org group, or {@code 0} if the group does not exist,
   * is not active, is not an org group, or the caller is not an administrator
   * @since 1.22.0
   */
  @Transactional(readOnly = true)
  public int orgGroupMemberCount(String groupId, String actingAdminUserId) {
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return 0;
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return 0;
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty() || maybeGroup.get().type() != GroupType.ORG) {
      return 0;
    }
    return maybeGroup.get().memberships().size();
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
    // Never-empty invariant (PO model): the OWNER is the permanent anchor of an ad-hoc group and
    // cannot self-remove. The owner's leave is governed by the guarded transfer-or-dissolve of
    // story FEAT-USER-GROUPS-05.
    if (roleOf(group.get(), userId).orElse(null) == GroupRole.OWNER) {
      return Result.fromError(accessDenied(userId, groupId));
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
   * domain layer). The target user must exist and must not be a member yet.</p>
   *
   * @param groupId      the id of the group
   * @param actingUserId the user performing the operation (must hold OWNER or MANAGER)
   * @param userId       the user to add as a regular member; must reference an existing user
   * @return a {@link Result} with no value on success, or an error if the group does not exist,
   * the user to add does not exist, is already a member, or the acting user lacks the required
   * role
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
    // Authorization (moved here from the aggregate): only an OWNER or MANAGER may add members.
    Optional<GroupRole> actingRole = roleOf(group, actingUserId);
    if (actingRole.isEmpty() || actingRole.get() == GroupRole.MEMBER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    // A group membership references a real identity user (no FK on group_membership.user_id):
    // reject unknown user ids up front so a typo or stale UI state can never create a corrupt
    // membership row that no real user can ever act on.
    if (userInformationService.findById(userId).isEmpty()) {
      return Result.fromError(userNotFound(userId));
    }
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
    UserGroup group = maybeGroup.get();
    // Authorization (moved here from the aggregate): only an OWNER or MANAGER may remove other
    // members.
    Optional<GroupRole> actingRole = roleOf(group, actingUserId);
    if (actingRole.isEmpty() || actingRole.get() == GroupRole.MEMBER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    // A manager may not remove another manager (only an owner may); removing the OWNER is an
    // aggregate invariant and rejected there.
    Optional<GroupRole> targetRole = roleOf(group, userId);
    if (targetRole.isEmpty()) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is not a member of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    if (targetRole.get() == GroupRole.MANAGER && actingRole.get() == GroupRole.MANAGER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
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
    UserGroup group = maybeGroup.get();
    // Authorization (moved here from the aggregate): only the OWNER may appoint managers.
    if (roleOf(group, actingUserId).orElse(null) != GroupRole.OWNER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    if (roleOf(group, userId).isEmpty()) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is not a member of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
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
    UserGroup group = maybeGroup.get();
    // Authorization (moved here from the aggregate): only the OWNER may demote managers.
    if (roleOf(group, actingUserId).orElse(null) != GroupRole.OWNER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    if (roleOf(group, userId).orElse(null) != GroupRole.MANAGER) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is not a manager of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    Optional<UserGroup> updated = domainService.get().demoteManager(maybeGroup.get().id(),
        actingUserId, userId);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Appoints a user as a MANAGER of an <b>org</b> group (admin-governed, FEAT-USER-GROUPS-02).
   *
   * <p>Admin gate (AC1): the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted. The appointed
   * user must exist (mirroring {@link #addMember(String, String, String)}); a blank admin or
   * target user id, a dissolved or non-org group, or a target that is already a MANAGER yields
   * an error.</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId        the user to appoint (non-member or existing regular MEMBER)
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> appointOrgManager(String groupId,
      String actingAdminUserId, String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id.", ErrorCode.GENERAL, ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.type() != GroupType.ORG) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    if (userInformationService.findById(userId).isEmpty()) {
      return Result.fromError(userNotFound(userId));
    }
    if (group.memberships().stream().anyMatch(m -> m.userId().equals(userId)
        && m.role() == GroupRole.MANAGER)) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is already a manager of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    Optional<UserGroup> updated = domainService.get().appointOrgManager(group.id(),
        actingAdminUserId, userId, Instant.now());
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Removes a manager's (or member's) membership from an <b>org</b> group (admin-governed,
   * FEAT-USER-GROUPS-02).
   *
   * <p>Admin gate (AC4): the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted. The member's
   * membership is removed entirely (no demotion); removing the last manager keeps the group
   * ACTIVE and admin-governed (no OWNER row).</p>
   *
   * @param groupId       the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId        the manager (or member) to remove
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> removeOrgManager(String groupId,
      String actingAdminUserId, String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id.", ErrorCode.GENERAL, ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.type() != GroupType.ORG) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    if (userInformationService.findById(userId).isEmpty()) {
      return Result.fromError(userNotFound(userId));
    }
    Optional<UserGroup> updated = domainService.get().removeOrgManager(group.id(),
        actingAdminUserId, userId);
    if (updated.isEmpty()) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is not a member of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    return Result.fromValue(null);
  }

  /**
   * Adds a regular MEMBER to an <b>org</b> group (admin-governed, FEAT-USER-GROUPS-02).
   *
   * <p>Admin gate: the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted. Grants the
   * regular MEMBER role (an org MANAGER uses the role-gated {@link #addMember}; the QBiC admin
   * is not a member and therefore uses this owner-equivalent path).</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId         the user to add as a regular member
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> addOrgMember(String groupId,
      String actingAdminUserId, String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id.", ErrorCode.GENERAL, ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.type() != GroupType.ORG) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    if (userInformationService.findById(userId).isEmpty()) {
      return Result.fromError(userNotFound(userId));
    }
    if (group.memberships().stream().anyMatch(m -> m.userId().equals(userId))) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is already a member of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    Optional<UserGroup> updated = domainService.get().addOrgMember(group.id(),
        actingAdminUserId, userId, Instant.now());
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Removes a regular MEMBER (or manager) membership from an <b>org</b> group
   * (admin-governed, FEAT-USER-GROUPS-02).
   *
   * <p>Admin gate: the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted. The member's
   * membership is removed entirely (no demotion); removing the last manager keeps the group
   * ACTIVE and admin-governed (AC4, no OWNER row).</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId         the member (or manager) to remove
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> removeOrgMember(String groupId,
      String actingAdminUserId, String userId) {
    return removeOrgManager(groupId, actingAdminUserId, userId);
  }

  /**
   * Demotes a MANAGER back to a regular MEMBER of an <b>org</b> group (admin-governed,
   * FEAT-USER-GROUPS-02 — role assignment in the group roster).
   *
   * <p>Admin gate: the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted. The manager is
   * demoted to MEMBER <em>while staying in the group</em> (distinct from {@link #removeOrgMember}
   * which removes the membership entirely).</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param userId         the manager to demote
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> demoteOrgManager(String groupId,
      String actingAdminUserId, String userId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id.", ErrorCode.GENERAL, ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.type() != GroupType.ORG) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> updated = domainService.get().demoteOrgManager(group.id(),
        actingAdminUserId, userId);
    if (updated.isEmpty()) {
      return Result.fromError(new ApplicationException(
          "User " + userId + " is not a manager of group " + groupId, ErrorCode.GENERAL,
          ErrorParameters.empty()));
    }
    return Result.fromValue(null);
  }

  /**
   * Renames an <b>org</b> group (admin-governed, FEAT-USER-GROUPS-02).
   *
   * <p>Admin gate: the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted. Reuses the
   * exact duplicate-name flow of {@link #renameGroup}: a case-insensitive pre-check plus a
   * {@link DataIntegrityViolationException} race mapping, both yielding
   * {@link ErrorCode#DUPLICATE_GROUP_NAME}.</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param newName        the new group name
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted or the new name is already taken
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> renameOrgGroup(String groupId,
      String actingAdminUserId, GroupName newName) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id.", ErrorCode.GENERAL, ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.type() != GroupType.ORG) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> existing = groupRepository.findByNameIgnoreCase(newName);
    if (existing.isPresent() && !existing.get().id().equals(maybeGroup.get().id())) {
      return Result.fromError(duplicateNameApplicationError(newName.value()));
    }
    Optional<UserGroup> updated = domainService.get().renameOrgGroup(group.id(),
        actingAdminUserId, newName);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Updates the description of an <b>org</b> group (admin-governed, FEAT-USER-GROUPS-02).
   *
   * <p>Admin gate: the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted.</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @param newDescription the new group description
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> updateOrgGroupDescription(String groupId,
      String actingAdminUserId, GroupDescription newDescription) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id.", ErrorCode.GENERAL, ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.type() != GroupType.ORG) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> updated = domainService.get().updateOrgGroupDescription(group.id(),
        actingAdminUserId, newDescription);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    return Result.fromValue(null);
  }

  /**
   * Dissolves an <b>org</b> group (admin-governed, FEAT-USER-GROUPS-02).
   *
   * <p>Admin gate: the caller must be a QBiC administrator — enforced through the
   * {@link GroupAdministrationPermission} port <b>before any write</b>; otherwise an
   * {@link ErrorCode#ACCESS_DENIED} error is returned and nothing is persisted. Admin dissolve
   * is the only way an org group ends (org groups never auto-dissolve). The blast radius
   * (orphaned ACEs / membership rows) is handled by EPIC-6 story 13; this operation marks the
   * group dissolved and purges its roster.</p>
   *
   * @param groupId        the id of the org group
   * @param actingAdminUserId the user performing the operation (must be a QBiC administrator)
   * @return a {@link Result} with no value on success, or an error if the operation is not
   * permitted
   * @since 1.22.0
   */
  @Transactional
  public Result<Void, ApplicationException> dissolveOrgGroup(String groupId,
      String actingAdminUserId) {
    var domainService = DomainRegistry.instance().groupDomainService();
    if (domainService.isEmpty()) {
      return Result.fromError(systemFailure());
    }
    if (actingAdminUserId == null || actingAdminUserId.isBlank()) {
      return Result.fromError(new ApplicationException(
          "Invalid admin user id.", ErrorCode.GENERAL, ErrorParameters.empty()));
    }
    if (!groupAdministrationPermission.isAdmin(actingAdminUserId)) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> maybeGroup = resolveActiveGroup(groupId);
    if (maybeGroup.isEmpty()) {
      return Result.fromError(groupNotFound(groupId));
    }
    UserGroup group = maybeGroup.get();
    if (group.type() != GroupType.ORG) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
    }
    Optional<UserGroup> updated = domainService.get().dissolveOrgGroup(group.id(),
        actingAdminUserId);
    if (updated.isEmpty()) {
      return Result.fromError(accessDenied(actingAdminUserId, groupId));
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
    UserGroup group = maybeGroup.get();
    // Authorization (moved here from the aggregate): only the OWNER or a MANAGER may rename.
    GroupRole actingRole = roleOf(group, actingUserId).orElse(null);
    if (actingRole == null || actingRole == GroupRole.MEMBER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
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
    UserGroup group = maybeGroup.get();
    // Authorization (moved here from the aggregate): only the OWNER or a MANAGER may edit.
    GroupRole actingRole = roleOf(group, actingUserId).orElse(null);
    if (actingRole == null || actingRole == GroupRole.MEMBER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
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
    UserGroup group = maybeGroup.get();
    // Authorization (moved here from the aggregate): only the OWNER may dissolve an ad-hoc group
    // explicitly. (Org groups are dissolved by a system admin via dissolveOrgGroup.)
    if (roleOf(group, actingUserId).orElse(null) != GroupRole.OWNER) {
      return Result.fromError(accessDenied(actingUserId, groupId));
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
    // D6 (FEAT-USER-GROUPS-02): the QBiC admin has oversight over ORG rosters (strategy §3).
    // Ad-hoc rosters stay member-only; the public directory is never affected.
    boolean adminOversight = group.type() == GroupType.ORG
        && groupAdministrationPermission.isAdmin(viewerId);
    if (!isMember && !adminOversight) {
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

  /**
   * Resolves a user's role inside a group, if they are a member.
   *
   * <p>Authorization helper: the ad-hoc role gates (OWNER/MANAGER) live in this application
   * layer, not in the aggregate.</p>
   */
  private static Optional<GroupRole> roleOf(UserGroup group, String userId) {
    return group.memberships().stream()
        .filter(m -> m.userId().equals(userId))
        .map(GroupMembership::role)
        .findFirst();
  }

  private ApplicationException groupNotFound(String groupId) {
    return new ApplicationException(String.format(GROUP_NOT_FOUND_MESSAGE, groupId),
        ErrorCode.GENERAL, ErrorParameters.empty());
  }

  private static ApplicationException userNotFound(String userId) {
    return new ApplicationException("User " + userId + " not found.", ErrorCode.GENERAL,
        ErrorParameters.empty());
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
    // The caller is a member, so the roster size is within the visibility policy — and it
    // comes for free here because the memberships are already loaded with the aggregate.
    int memberCount = group.memberships().size();
    return new GroupMembershipProjection(group.id(), group.name(), group.description(),
        group.type(), role, memberCount);
  }

  private GroupInfoProjection toInfoProjection(UserGroup group) {
    return new GroupInfoProjection(group.id(), group.name(), group.description(), group.type());
  }
}