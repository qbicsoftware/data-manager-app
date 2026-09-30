package life.qbic.usergroups.domain.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import life.qbic.domain.concepts.DomainEvent;

/**
 * <b>A user has been added to a group.</b>
 *
 * <p>Published by the {@code GroupDomainService} after a member has been added to an ad-hoc
 * group by an OWNER or MANAGER. Carries the group id, the added user id and the user who
 * performed the addition.</p>
 *
 * <p>Consumed by the notification profile (GROUP-R-10): a newly added member may gain effective
 * project access through the group's grants and receives a <em>newly-gained-access</em> email
 * (deduplicated); members who joined a group that already carries grants also receive a join
 * digest.</p>
 *
 * @since 1.20.0
 */
public class MemberAddedToGroup extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 5234567890123456702L;

  @JsonProperty("groupId")
  private String groupId;
  @JsonProperty("userId")
  private String userId;
  @JsonProperty("triggeredByUserId")
  private String triggeredByUserId;

  protected MemberAddedToGroup() {
    // for deserialization
  }

  /**
   * Creates a new {@link MemberAddedToGroup} event.
   *
   * @param groupId           the id of the group
   * @param userId            the id of the newly added user
   * @param triggeredByUserId the user who performed the addition
   * @return the new event
   * @since 1.20.0
   */
  public static MemberAddedToGroup create(String groupId, String userId,
      String triggeredByUserId) {
    return new MemberAddedToGroup(groupId, userId, triggeredByUserId);
  }

  private MemberAddedToGroup(String groupId, String userId, String triggeredByUserId) {
    this.groupId = groupId;
    this.userId = userId;
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

  @JsonGetter("triggeredByUserId")
  public String triggeredByUserId() {
    return triggeredByUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MemberAddedToGroup that)) {
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
    return triggeredByUserId.equals(that.triggeredByUserId);
  }

  @Override
  public int hashCode() {
    int result = occurredOn.hashCode();
    result = 31 * result + groupId.hashCode();
    result = 31 * result + userId.hashCode();
    result = 31 * result + triggeredByUserId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "MemberAddedToGroup{" +
        "occurredOn=" + occurredOn +
        ", groupId='" + groupId + '\'' +
        ", userId='" + userId + '\'' +
        ", triggeredByUserId='" + triggeredByUserId + '\'' +
        '}';
  }
}