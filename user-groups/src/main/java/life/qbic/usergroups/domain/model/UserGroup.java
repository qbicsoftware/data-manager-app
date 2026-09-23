package life.qbic.usergroups.domain.model;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import life.qbic.usergroups.domain.model.translation.GroupDescriptionConverter;
import life.qbic.usergroups.domain.model.translation.GroupNameConverter;

/**
 * <b>User group</b>
 * <p>
 * Aggregate root of the user groups context. A group has a unique (case-insensitive) name, an
 * optional description, a type ({@link GroupType#ORG} vs {@link GroupType#ADHOC}), a lifecycle
 * status and an owned membership roster.
 *
 * <p>Role structure <em>inside</em> the group: OWNER (ad-hoc creator) / MANAGER / MEMBER. This is
 * unrelated to project access roles (Spring ACL).
 *
 * <p><em>Soft dissolve:</em> when the last membership of an ad-hoc group is removed, the group is
 * dissolved by setting the status to {@link GroupStatus#DISSOLVED} and purging all memberships.
 * The {@code user_group} row itself is never deleted.
 *
 * @since 1.19.0
 */
@Entity
@Table(name = "user_group")
public class UserGroup implements Serializable {

  @Serial
  private static final long serialVersionUID = 8901234567890123456L;

  @EmbeddedId
  private GroupId id;

  @Convert(converter = GroupNameConverter.class)
  @Column(name = "name")
  private GroupName name;

  @Convert(converter = GroupDescriptionConverter.class)
  @Column(name = "description")
  private GroupDescription description;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private GroupType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private GroupStatus status;

  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "created_at")
  private Instant createdAt;

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GroupMembership> memberships = new ArrayList<>();

  protected UserGroup() {
    // for JPA
  }

  private UserGroup(GroupId id, GroupName name, GroupDescription description, GroupType type,
      String createdBy, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.status = GroupStatus.ACTIVE;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  /**
   * Creates a new ad-hoc user group with the creator as its sole owner.
   *
   * <p>Invariants: type is {@link GroupType#ADHOC}, status is {@link GroupStatus#ACTIVE}, and the
   * roster contains exactly one membership: the creator with role {@link GroupRole#OWNER}.
   *
   * @param id            the new group id
   * @param name          the group name
   * @param description   the group description (optional)
   * @param creatorUserId the user id of the creating user
   * @param createdAt     the creation timestamp
   * @return the new ad-hoc group
   * @throws IllegalArgumentException if the creator user id is null or blank
   * @since 1.19.0
   */
  public static UserGroup createAdHoc(GroupId id, GroupName name, GroupDescription description,
      String creatorUserId, Instant createdAt) {
    requireNonNull(id, "id must not be null");
    requireNonNull(name, "name must not be null");
    requireNonNull(description, "description must not be null (use GroupDescription.from(null))");
    requireNonNull(createdAt, "createdAt must not be null");
    if (creatorUserId == null || creatorUserId.isBlank()) {
      throw new IllegalArgumentException("creatorUserId must not be null or blank");
    }
    UserGroup group = new UserGroup(id, name, description, GroupType.ADHOC, creatorUserId,
        createdAt);
    GroupMembership ownerMembership = GroupMembership.create(id, creatorUserId, GroupRole.OWNER,
        createdAt);
    ownerMembership.attachTo(group);
    group.memberships.add(ownerMembership);
    return group;
  }

  /**
   * Removes a user's membership from this group.
   *
   * <p>If this is an ad-hoc group and the roster becomes empty, the group is dissolved: the
   * status is set to {@link GroupStatus#DISSOLVED} and all memberships are purged (soft dissolve,
   * the row is kept).
   *
   * @param userId the user id to remove
   * @return {@code true} if the group was dissolved as a result of this removal
   * @since 1.19.0
   */
  public boolean removeMembership(String userId) {
    GroupMembership membership = findMembership(userId);
    if (membership == null) {
      return false;
    }
    membership.detach();
    this.memberships.remove(membership);
    if (type == GroupType.ADHOC && memberships.isEmpty()) {
      dissolve();
      return true;
    }
    return false;
  }

  private GroupMembership findMembership(String userId) {
    for (GroupMembership membership : memberships) {
      if (membership.userId().equals(userId)) {
        return membership;
      }
    }
    return null;
  }

  /**
   * Adds a regular MEMBER to this group.
   *
   * <p>This operation is role-gated: only the OWNER or a MANAGER may add regular members
   * (FEAT-USER-GROUPS-04). Org groups are admin-governed and do not expose this operation to
   * their members (the QBiC admin acts as owner-equivalent at the application layer).</p>
   *
   * @param actingUserId the user performing the operation; must hold role OWNER or MANAGER
   *                     inside this group
   * @param userId       the user id to add
   * @param joinedAt     the join timestamp
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the acting user lacks OWNER/MANAGER role, or the user to
   *                                  add is already a member
   * @since 1.20.0
   */
  public void addMember(String actingUserId, String userId, Instant joinedAt) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot add members to a dissolved group");
    }
    requireRole(actingUserId, "Only the group owner or a manager may add members",
        GroupRole.OWNER, GroupRole.MANAGER);
    if (findMembership(userId) != null) {
      throw new IllegalArgumentException("User " + userId + " is already a member of this group");
    }
    GroupMembership membership = GroupMembership.create(id, userId, GroupRole.MEMBER, joinedAt);
    membership.attachTo(this);
    memberships.add(membership);
  }

  /**
   * Removes a regular MEMBER from this group.
   *
   * <p>Role-gated: only the OWNER or a MANAGER may remove <em>other</em> members. Any role may
   * remove themselves (self-remove, see {@link #removeMembership(String)}). When the removal
   * empties an ad-hoc group the group is auto-dissolved.</p>
   *
   * @param actingUserId the user performing the removal
   * @param userId       the user to remove; must not equal the acting user (use
   *                     {@link #removeMembership(String)} for self-removal)
   * @return {@code true} if the group was dissolved as a result
   * @throws IllegalArgumentException if the acting user may not remove the given member, the
   *                                  target is not a member, or the acting user tries to remove
   *                                  themselves
   * @since 1.20.0
   */
  public boolean removeMember(String actingUserId, String userId) {
    if (actingUserId.equals(userId)) {
      throw new IllegalArgumentException(
          "Use removeMembership(userId) to remove yourself from a group");
    }
    if (findMembership(actingUserId) == null) {
      throw new IllegalArgumentException("Acting user " + actingUserId
          + " is not a member of this group");
    }
    GroupRole actingRole = findMembership(actingUserId).role();
    if (actingRole == GroupRole.MEMBER) {
      throw new IllegalArgumentException(
          "Only the group owner or a manager may remove other members");
    }
    GroupMembership target = findMembership(userId);
    if (target == null) {
      throw new IllegalArgumentException("User " + userId + " is not a member of this group");
    }
    if (target.role() == GroupRole.OWNER) {
      throw new IllegalArgumentException("The group owner cannot be removed");
    }
    if (target.role() == GroupRole.MANAGER && actingRole != GroupRole.OWNER) {
      throw new IllegalArgumentException("Only the group owner may remove a manager");
    }
    boolean removed = this.memberships.remove(target);
    if (!removed) {
      return false;
    }
    if (type == GroupType.ADHOC && memberships.isEmpty()) {
      dissolve();
      return true;
    }
    return false;
  }

  /**
   * Appoints a regular member as a MANAGER of this group.
   *
   * <p>Role-gated: only the OWNER may appoint managers (ad-hoc groups). The appointed user must
   * already be a member.</p>
   *
   * @param actingUserId the user performing the operation; must hold role OWNER
   * @param userId       the member to promote to MANAGER
   * @return whether the membership role changed
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the acting user is not the OWNER, the target user is not
   *                                  a member, or the target is the OWNER themselves
   * @since 1.20.0
   */
  public boolean appointManager(String actingUserId, String userId) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot manage a dissolved group");
    }
    requireRole(actingUserId, "Only the group owner may appoint managers", GroupRole.OWNER);
    GroupMembership target = findMembership(userId);
    if (target == null) {
      throw new IllegalArgumentException("User " + userId + " is not a member of this group");
    }
    if (target.role() == GroupRole.OWNER) {
      throw new IllegalArgumentException("The group owner cannot be a manager");
    }
    if (target.role() == GroupRole.MANAGER) {
      return false;
    }
    target.setRole(GroupRole.MANAGER);
    return true;
  }

  /**
   * Demotes a manager back to a regular MEMBER.
   *
   * <p>Role-gated: only the OWNER may demote managers.</p>
   *
   * @param actingUserId the user performing the operation; must hold role OWNER
   * @param userId       the manager to demote
   * @return whether the membership role changed
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the acting user is not the OWNER, the target is not a
   *                                  manager, or the target is the OWNER themselves
   * @since 1.20.0
   */
  public boolean demoteManager(String actingUserId, String userId) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot manage a dissolved group");
    }
    requireRole(actingUserId, "Only the group owner may demote managers", GroupRole.OWNER);
    GroupMembership target = findMembership(userId);
    if (target == null) {
      throw new IllegalArgumentException("User " + userId + " is not a member of this group");
    }
    if (target.role() != GroupRole.MANAGER) {
      throw new IllegalArgumentException("User " + userId + " is not a manager of this group");
    }
    target.setRole(GroupRole.MEMBER);
    return true;
  }

  /**
   * Renames this group.
   *
   * <p>Role-gated: the OWNER and MANAGER may rename an ad-hoc group (FEAT-USER-GROUPS-04,
   * AC 'manager can rename/describe'). The new name must be unique case-insensitively, which is
   * enforced by the application layer.</p>
   *
   * @param actingUserId the user performing the operation; must hold role OWNER or MANAGER
   * @param newName      the new group name
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the acting user lacks OWNER/MANAGER role
   * @since 1.20.0
   */
  public void rename(String actingUserId, GroupName newName) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot rename a dissolved group");
    }
    requireRole(actingUserId, "Only the group owner or a manager may rename the group",
        GroupRole.OWNER, GroupRole.MANAGER);
    this.name = requireNonNull(newName, "newName must not be null");
  }

  /**
   * Updates the group description.
   *
   * <p>Role-gated: the OWNER and MANAGER may change the description of an ad-hoc group.</p>
   *
   * @param actingUserId   the user performing the operation; must hold role OWNER or MANAGER
   * @param newDescription the new group description (may be empty, never {@code null})
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the acting user lacks OWNER/MANAGER role
   * @since 1.20.0
   */
  public void updateDescription(String actingUserId, GroupDescription newDescription) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot change the description of a dissolved group");
    }
    requireRole(actingUserId, "Only the group owner or a manager may change the group description",
        GroupRole.OWNER, GroupRole.MANAGER);
    this.description = requireNonNull(newDescription, "newDescription must not be null");
  }

  /**
   * Dissolves this group by its owner (explicit dissolve with confirmation in the UI).
   *
   * <p>Role-gated: only the OWNER may dissolve an ad-hoc group explicitly. This is distinct from
   * the automatic dissolve when the last membership leaves ({@link #removeMembership(String)}).</p>
   *
   * @param actingUserId the user performing the operation; must hold role OWNER
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the acting user is not the OWNER
   * @since 1.20.0
   */
  public void dissolveByOwner(String actingUserId) {
    if (status == GroupStatus.DISSOLVED) {
      return;
    }
    requireRole(actingUserId, "Only the group owner may dissolve the group", GroupRole.OWNER);
    dissolve();
  }

  /**
   * Asserts that the acting user holds at least one of the given roles. Every membership-based
   * management operation funnels through this check so role gates stay consistent.
   *
   * @param actingUserId the user performing the operation
   * @param allowed      the roles that are allowed to perform the operation
   */
  private void requireRole(String actingUserId, String errorMessage, GroupRole... allowed) {
    GroupMembership membership = findMembership(actingUserId);
    if (membership == null) {
      throw new IllegalArgumentException("User " + actingUserId
          + " is not a member of this group");
    }
    for (GroupRole role : allowed) {
      if (membership.role() == role) {
        return;
      }
    }
    throw new IllegalArgumentException(errorMessage);
  }

  /**
   * Dissolves this group (soft): marks it {@link GroupStatus#DISSOLVED} and purges all
   * memberships. The row is kept for traceability.
   *
   * @since 1.19.0
   */
  public void dissolve() {
    if (status == GroupStatus.DISSOLVED) {
      return;
    }
    this.status = GroupStatus.DISSOLVED;
    for (GroupMembership membership : memberships) {
      membership.detach();
    }
    memberships.clear();
  }

  public GroupId id() {
    return id;
  }

  public GroupName name() {
    return name;
  }

  public GroupDescription description() {
    return description;
  }

  public GroupType type() {
    return type;
  }

  public GroupStatus status() {
    return status;
  }

  public String createdBy() {
    return createdBy;
  }

  public Instant createdAt() {
    return createdAt;
  }

  /**
   * Returns the membership roster of this group.
   *
   * @return immutable view of the memberships
   * @since 1.19.0
   */
  public List<GroupMembership> memberships() {
    return Collections.unmodifiableList(memberships);
  }

  public boolean isActive() {
    return status == GroupStatus.ACTIVE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserGroup userGroup = (UserGroup) o;
    return Objects.equals(id, userGroup.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "UserGroup{" +
        "id='" + id + '\'' +
        ", name=" + name +
        ", type=" + type +
        ", status=" + status +
        ", createdBy='" + createdBy + '\'' +
        ", createdAt=" + createdAt +
        '}';
  }
}