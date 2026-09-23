package life.qbic.usergroups.domain.model;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
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

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "group_id")
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
    ownerMembership.attachTo();
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
   * Adds a member (package-private; the full add-member flow with role policies and events lives
   * in the application layer in later tasks). Keeps the bidirectional association consistent.
   *
   * @param userId   the user id to add
   * @param role     the role inside the group
   * @param joinedAt the join timestamp
   * @since 1.19.0
   */
  void addMember(String userId, GroupRole role, Instant joinedAt) {
    if (status == GroupStatus.DISSOLVED) {
      throw new IllegalStateException("Cannot add members to a dissolved group");
    }
    if (findMembership(userId) != null) {
      throw new IllegalArgumentException("User " + userId + " is already a member of this group");
    }
    GroupMembership membership = GroupMembership.create(id, userId, role, joinedAt);
    membership.attachTo();
    memberships.add(membership);
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