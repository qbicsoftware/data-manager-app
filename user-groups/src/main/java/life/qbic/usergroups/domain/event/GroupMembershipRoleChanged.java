package life.qbic.usergroups.domain.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.usergroups.domain.model.GroupRole;

/**
 * <b>A user's role inside a group has changed.</b>
 *
 * <p>Published by the {@code GroupDomainService} after the OWNER appoints or demotes a MANAGER
 * (or when a role otherwise changes) and the change has been persisted. Carries the group id, the
 * affected user id, the previous role and the new role.</p>
 *
 * <p>Per the notification profile (GROUP-R-10), role-level changes inside a group are
 * <em>audit-log-only</em> — no email is sent. This event is the audit hook.</p>
 *
 * @since 1.20.0
 */
public class GroupMembershipRoleChanged extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 5234567890123456701L;

  @JsonProperty("groupId")
  private String groupId;
  @JsonProperty("userId")
  private String userId;
  @JsonProperty("previousRole")
  private GroupRole previousRole;
  @JsonProperty("newRole")
  private GroupRole newRole;
  @JsonProperty("triggeredByUserId")
  private String triggeredByUserId;

  protected GroupMembershipRoleChanged() {
    // for deserialization
  }

  /**
   * Creates a new {@link GroupMembershipRoleChanged} event.
   *
   * @param groupId           the id of the group
   * @param userId            the user whose role changed
   * @param previousRole      the role before the change
   * @param newRole           the role after the change
   * @param triggeredByUserId the user who triggered the change (the group OWNER)
   * @return the new event
   * @since 1.20.0
   */
  public static GroupMembershipRoleChanged create(String groupId, String userId,
      GroupRole previousRole, GroupRole newRole, String triggeredByUserId) {
    return new GroupMembershipRoleChanged(groupId, userId, previousRole, newRole,
        triggeredByUserId);
  }

  private GroupMembershipRoleChanged(String groupId, String userId, GroupRole previousRole,
      GroupRole newRole, String triggeredByUserId) {
    this.groupId = groupId;
    this.userId = userId;
    this.previousRole = previousRole;
    this.newRole = newRole;
    this.triggeredByUserId = triggeredByUserId;
  }

  @JsonGetter("groupId")
  public String groupId() {
    return groupId;
  }

  @JsonGetter("userId")
  public String userId() {
    return userId;
  }

  @JsonGetter("previousRole")
  public GroupRole previousRole() {
    return previousRole;
  }

  @JsonGetter("newRole")
  public GroupRole newRole() {
    return newRole;
  }

  @JsonGetter("triggeredByUserId")
  public String triggeredByUserId() {
    return triggeredByUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GroupMembershipRoleChanged that)) {
      return false;
    }
    if (!occurredOn.equals(that.occurredOn)) {
      return false;
    }
    if (!groupId.equals(that.groupId)) {
      return false;
    }
    if (!userId.equals(that.userId)) {
      return false;
    }
    if (previousRole != that.previousRole) {
      return false;
    }
    if (newRole != that.newRole) {
      return false;
    }
    return triggeredByUserId.equals(that.triggeredByUserId);
  }

  @Override
  public int hashCode() {
    int result = occurredOn.hashCode();
    result = 31 * result + groupId.hashCode();
    result = 31 * result + userId.hashCode();
    result = 31 * result + previousRole.hashCode();
    result = 31 * result + newRole.hashCode();
    result = 31 * result + triggeredByUserId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "GroupMembershipRoleChanged{" +
        "occurredOn=" + occurredOn +
        ", groupId='" + groupId + '\'' +
        ", userId='" + userId + '\'' +
        ", previousRole=" + previousRole +
        ", newRole=" + newRole +
        ", triggeredByUserId='" + triggeredByUserId + '\'' +
        '}';
  }
}