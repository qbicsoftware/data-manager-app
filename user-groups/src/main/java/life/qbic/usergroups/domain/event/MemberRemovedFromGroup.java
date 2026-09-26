package life.qbic.usergroups.domain.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import life.qbic.domain.concepts.DomainEvent;

/**
 * <b>A user has been removed from a group by a manager or owner.</b>
 *
 * <p>Published by the {@code GroupDomainService} after a member has been removed from an ad-hoc
 * group by an OWNER or MANAGER (distinct from a voluntary self-remove, which flows through
 * {@link GroupDissolved} only when it empties the group).</p>
 *
 * <p>Consumed by the notification profile (GROUP-R-10): the removed user receives a revocation
 * email; group managers receive an in-app notice; project owners/admins of affected projects are
 * informed about the membership change.</p>
 *
 * @since 1.20.0
 */
public class MemberRemovedFromGroup extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 5234567890123456703L;

  @JsonProperty("groupId")
  private String groupId;
  @JsonProperty("userId")
  private String userId;
  @JsonProperty("triggeredByUserId")
  private String triggeredByUserId;

  protected MemberRemovedFromGroup() {
    // for deserialization
  }

  /**
   * Creates a new {@link MemberRemovedFromGroup} event.
   *
   * @param groupId           the id of the group
   * @param userId            the id of the removed user
   * @param triggeredByUserId the user who performed the removal
   * @return the new event
   * @since 1.20.0
   */
  public static MemberRemovedFromGroup create(String groupId, String userId,
      String triggeredByUserId) {
    return new MemberRemovedFromGroup(groupId, userId, triggeredByUserId);
  }

  private MemberRemovedFromGroup(String groupId, String userId, String triggeredByUserId) {
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
    if (!(o instanceof MemberRemovedFromGroup that)) {
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
    return "MemberRemovedFromGroup{" +
        "occurredOn=" + occurredOn +
        ", groupId='" + groupId + '\'' +
        ", userId='" + userId + '\'' +
        ", triggeredByUserId='" + triggeredByUserId + '\'' +
        '}';
  }
}