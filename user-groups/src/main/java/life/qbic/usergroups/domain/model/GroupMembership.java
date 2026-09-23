package life.qbic.usergroups.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * <b>Group membership</b>
 * <p>
 * Owned child entity of {@link UserGroup} representing a single user's membership of a group,
 * including the role the user holds inside the group and when they joined.
 *
 * <p>Identity is the composite {@link GroupMembershipId} {@code (groupId, userId)}. The list of
 * memberships is owned by the {@link UserGroup} aggregate and mapped via a unidirectional
 * {@code @OneToMany} with {@code orphanRemoval}.
 *
 * @since 1.19.0
 */
@Entity
@Table(name = "group_membership")
public class GroupMembership implements Serializable {

  @Serial
  private static final long serialVersionUID = 7890123456789012345L;

  @EmbeddedId
  private GroupMembershipId id;

  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  private GroupRole role;

  @Column(name = "joined_at")
  private Instant joinedAt;

  protected GroupMembership() {
    // for JPA
  }

  private GroupMembership(GroupMembershipId id, GroupRole role, Instant joinedAt) {
    this.id = id;
    this.role = role;
    this.joinedAt = joinedAt;
  }

  /**
   * Creates a new membership.
   *
   * @param groupId  the group id
   * @param userId   the user id
   * @param role     the role inside the group
   * @param joinedAt when the user joined
   * @return the new membership
   * @since 1.19.0
   */
  public static GroupMembership create(GroupId groupId, String userId, GroupRole role,
      Instant joinedAt) {
    return new GroupMembership(new GroupMembershipId(groupId.get(), userId), role, joinedAt);
  }

  void attachTo() {
    // no-op: membership's composite key carries the group id; no back-reference to the aggregate
    // is needed (the collection is owned by UserGroup via a unidirectional @OneToMany).
  }

  /**
   * Detaches this membership from its owning group (package-private).
   *
   * @since 1.19.0
   */
  void detach() {
    // no-op: see {@link #attachTo()}.
  }

  /**
   * Updates the role of this membership (package-private; the aggregate funnels role changes
   * through its role-gated management operations).
   *
   * @param newRole the new role inside the group
   * @since 1.20.0
   */
  void setRole(GroupRole newRole) {
    this.role = Objects.requireNonNull(newRole, "newRole must not be null");
  }

  public GroupMembershipId id() {
    return id;
  }

  public String userId() {
    return id.userId();
  }

  public GroupRole role() {
    return role;
  }

  public Instant joinedAt() {
    return joinedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupMembership that = (GroupMembership) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "GroupMembership{" +
        "id=" + id +
        ", role=" + role +
        ", joinedAt=" + joinedAt +
        '}';
  }
}