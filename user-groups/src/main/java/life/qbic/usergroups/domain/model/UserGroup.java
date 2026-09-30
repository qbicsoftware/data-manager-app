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
 * <p><b>Type-agnostic aggregate.</b> This aggregate does <em>not</em> branch on {@link GroupType}:
 * its operations are the unconditional invariants shared by both group types (roster integrity,
 * the "the last member is always the OWNER" invariant, the lifecycle status). Who is allowed to
 * perform an operation is <em>authorization</em> and therefore lives at the application boundary
 * ({@code GroupService}), not here — the two group types differ only in who acts as
 * owner-equivalent: the ad-hoc creator-OWNER membership vs. any QBiC system administrator for
 * org groups.
 *
 * <p><b>Never empty, never auto-dissolved.</b> An ad-hoc group is always created with its OWNER
 * and the OWNER can never be removed, so the roster never becomes empty through member removal.
 * The group is dissolved only by an <em>explicit</em> operation ({@link #dissolve()}) — never
 * automatically. Org groups may start with an empty roster (no OWNER row, governed by system
 * admins) and likewise only end by an explicit admin dissolve.
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
   * roster contains exactly one membership: the creator with role {@link GroupRole#OWNER}. The
   * owner is the permanent anchor of the group — it can never be removed, so the group is never
   * empty through member removal.</p>
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
   * Creates a new organisational user group.
   *
   * <p>Invariants: type is {@link GroupType#ORG}, status is {@link GroupStatus#ACTIVE}, and the
   * roster is <b>empty</b> — org groups have <em>no OWNER membership row</em> by design
   * (user-groups strategy §3/§4.2): any QBiC system administrator acts as owner-equivalent at
   * the application layer (system role {@code ROLE_ADMIN}), never as a group member. The creating
   * admin's id is retained on the {@code created_by} column for traceability.</p>
   *
   * @param id            the new group id
   * @param name          the group name
   * @param description   the group description (optional)
   * @param createdByUserId the user id of the creating QBiC administrator
   * @param createdAt     the creation timestamp
   * @return the new org group with an empty roster
   * @throws IllegalArgumentException if the creating user id is null or blank
   * @since 1.21.0
   */
  public static UserGroup createOrg(GroupId id, GroupName name, GroupDescription description,
      String createdByUserId, Instant createdAt) {
    requireNonNull(id, "id must not be null");
    requireNonNull(name, "name must not be null");
    requireNonNull(description, "description must not be null (use GroupDescription.from(null))");
    requireNonNull(createdAt, "createdAt must not be null");
    if (createdByUserId == null || createdByUserId.isBlank()) {
      throw new IllegalArgumentException("createdByUserId must not be null or blank");
    }
    return new UserGroup(id, name, description, GroupType.ORG, createdByUserId, createdAt);
  }

  /**
   * Removes a user's membership from this group (self-removal).
   *
   * <p>The group is <b>never</b> dissolved by this operation — removing a member simply removes
   * their membership and keeps the group ACTIVE. (For ad-hoc groups the OWNER can never be
   * removed, so the roster never becomes empty this way; the owner's leave is the guarded
   * transfer-or-dissolve of story FEAT-USER-GROUPS-05.)</p>
   *
   * @param userId the user id to remove
   * @return {@code true} if a membership was removed, {@code false} if the user was not a member
   * @since 1.19.0
   */
  public boolean removeMembership(String userId) {
    GroupMembership membership = findMembership(userId);
    if (membership == null) {
      return false;
    }
    membership.detach();
    this.memberships.remove(membership);
    return true;
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
   * Adds a regular member to this group.
   *
   * <p>No authorization lives here: who may add members is decided at the application layer
   * (an ad-hoc OWNER/MANAGER, or a system admin acting as owner-equivalent for an org group).
   * This method only upholds the roster invariant — the target must not already be a member.</p>
   *
   * @param userId   the user id to add as a regular member
   * @param joinedAt the join timestamp
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the user to add is already a member
   * @since 1.20.0
   */
  public void addMember(String userId, Instant joinedAt) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot add members to a dissolved group");
    }
    if (findMembership(userId) != null) {
      throw new IllegalArgumentException("User " + userId + " is already a member of this group");
    }
    GroupMembership membership = GroupMembership.create(id, userId, GroupRole.MEMBER, joinedAt);
    membership.attachTo(this);
    memberships.add(membership);
  }

  /**
   * Removes another member's membership from this group.
   *
   * <p>No authorization lives here (who may remove is the application layer's concern). The two
   * guards below are <em>unconditional invariants</em>, not role gates:
   * <ul>
   *   <li>the acting user must not be removing themselves (self-removal goes through
   *   {@link #removeMembership(String)}), and</li>
   *   <li>the OWNER can never be removed — the owner is the permanent anchor, so the roster can
   *   never become empty this way (ad-hoc).</li>
   * </ul>
   * The group is never dissolved by this operation.</p>
   *
   * @param actingUserId the user performing the removal (must not be the removal target)
   * @param userId       the member to remove (must not be the OWNER)
   * @return {@code true} if a membership was removed, {@code false} if the target was not a
   * member
   * @throws IllegalArgumentException if the acting user removes themselves, or the target is the
   *                                  OWNER
   * @since 1.20.0
   */
  public boolean removeMember(String actingUserId, String userId) {
    if (actingUserId.equals(userId)) {
      throw new IllegalArgumentException(
          "Use removeMembership(userId) to remove yourself from a group");
    }
    GroupMembership target = findMembership(userId);
    if (target == null) {
      return false;
    }
    if (target.role() == GroupRole.OWNER) {
      throw new IllegalArgumentException("The group owner cannot be removed");
    }
    this.memberships.remove(target);
    target.detach();
    return true;
  }

  /**
   * Changes the role of an existing member.
   *
   * <p>No authorization lives here (appoint/demote authority is the application layer's
   * concern). This is the primitive behind appointing a MANAGER and demoting back to MEMBER for
   * both group types.</p>
   *
   * @param userId  the member whose role changes
   * @param newRole the new role (never {@link GroupRole#OWNER}; the OWNER role cannot be granted
   *                and the owner membership cannot be re-rolled)
   * @return {@code true} if the role changed, {@code false} if the member already holds it
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the user is not a member, or {@code newRole} is
   *                                  {@link GroupRole#OWNER}
   * @since 1.20.0
   */
  public boolean setRoleOf(String userId, GroupRole newRole) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot manage a dissolved group");
    }
    GroupMembership target = findMembership(userId);
    if (target == null) {
      throw new IllegalArgumentException("User " + userId + " is not a member of this group");
    }
    if (newRole == GroupRole.OWNER) {
      throw new IllegalArgumentException("The role OWNER cannot be assigned via setRoleOf");
    }
    if (target.role() == newRole) {
      return false;
    }
    target.setRole(newRole);
    return true;
  }

  /**
   * Renames this group.
   *
   * <p>No authorization lives here (who may rename is the application layer's concern). The
   * unique case-insensitive name invariant is enforced by the application layer + database.</p>
   *
   * @param newName the new group name
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the new name is null
   * @since 1.20.0
   */
  public void rename(GroupName newName) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot rename a dissolved group");
    }
    this.name = requireNonNull(newName, "newName must not be null");
  }

  /**
   * Updates the group description.
   *
   * <p>No authorization lives here (who may edit is the application layer's concern).</p>
   *
   * @param newDescription the new group description (may be empty, never {@code null})
   * @throws IllegalStateException    if the group is dissolved
   * @throws IllegalArgumentException if the new description is null
   * @since 1.20.0
   */
  public void updateDescription(GroupDescription newDescription) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot change the description of a dissolved group");
    }
    this.description = requireNonNull(newDescription, "newDescription must not be null");
  }

  /**
   * Dissolves this group (soft): marks it {@link GroupStatus#DISSOLVED} and purges all
   * memberships. The row is kept for traceability.
   *
   * <p>This is the <em>only</em> way a group is dissolved — an explicit, authorized operation
   * (ad-hoc owner dissolve, admin org dissolve, or story-05 transfer-or-dissolve). There is no
   * auto-dissolve anywhere.</p>
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