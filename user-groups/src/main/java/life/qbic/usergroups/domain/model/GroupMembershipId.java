package life.qbic.usergroups.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * <b>Group membership id</b>
 * <p>
 * Composite primary key of a {@link GroupMembership}: the pair {@code (groupId, userId)}. Each
 * user holds exactly one membership per group.
 *
 * @since 1.19.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class GroupMembershipId implements Serializable {

  @Serial
  private static final long serialVersionUID = 6789012345678901234L;

  @Column(name = "group_id")
  private String groupId;

  @Column(name = "user_id")
  private String userId;

  protected GroupMembershipId() {
    // for JPA
  }

  public GroupMembershipId(String groupId, String userId) {
    if (groupId == null || groupId.isBlank()) {
      throw new IllegalArgumentException("groupId must not be null or blank");
    }
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be null or blank");
    }
    this.groupId = groupId;
    this.userId = userId;
  }

  public String groupId() {
    return groupId;
  }

  public String userId() {
    return userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupMembershipId that = (GroupMembershipId) o;
    return Objects.equals(groupId, that.groupId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, userId);
  }

  @Override
  public String toString() {
    return "GroupMembershipId{" +
        "groupId='" + groupId + '\'' +
        ", userId='" + userId + '\'' +
        '}';
  }
}
