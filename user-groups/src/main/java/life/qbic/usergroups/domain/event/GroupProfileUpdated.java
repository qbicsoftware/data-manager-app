package life.qbic.usergroups.domain.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import life.qbic.domain.concepts.DomainEvent;

/**
 * <b>A group's profile (name and/or description) has been changed.</b>
 *
 * <p>Published by the {@code GroupDomainService} after a rename or description change has been
 * persisted. Carries the group id, the old and new names and the user who performed the change.</p>
 *
 * <p>Consumed by the notification profile (GROUP-R-10): project owners/admins of affected groups
 * are informed about the membership/profile change (audit trail).</p>
 *
 * @since 1.20.0
 */
public class GroupProfileUpdated extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 5234567890123456704L;

  @JsonProperty("groupId")
  private String groupId;
  @JsonProperty("oldName")
  private String oldName;
  @JsonProperty("newName")
  private String newName;
  @JsonProperty("triggeredByUserId")
  private String triggeredByUserId;

  protected GroupProfileUpdated() {
    // for deserialization
  }

  /**
   * Creates a new {@link GroupProfileUpdated} event.
   *
   * @param groupId           the id of the group
   * @param oldName           the group name before the change
   * @param newName           the group name after the change
   * @param triggeredByUserId the user who performed the change
   * @return the new event
   * @since 1.20.0
   */
  public static GroupProfileUpdated create(String groupId, String oldName, String newName,
      String triggeredByUserId) {
    return new GroupProfileUpdated(groupId, oldName, newName, triggeredByUserId);
  }

  private GroupProfileUpdated(String groupId, String oldName, String newName,
      String triggeredByUserId) {
    this.groupId = groupId;
    this.oldName = oldName;
    this.newName = newName;
    this.triggeredByUserId = triggeredByUserId;
  }

  @JsonGetter("groupId")
  public String groupId() {
    return groupId;
  }

  @JsonGetter("oldName")
  public String oldName() {
    return oldName;
  }

  @JsonGetter("newName")
  public String newName() {
    return newName;
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
    if (!(o instanceof GroupProfileUpdated that)) {
      return false;
    }
    if (!occurredOn.equals(that.occurredOn)) {
      return false;
    }
    if (!groupId.equals(that.groupId)) {
      return false;
    }
    if (!oldName.equals(that.oldName)) {
      return false;
    }
    if (!newName.equals(that.newName)) {
      return false;
    }
    return triggeredByUserId.equals(that.triggeredByUserId);
  }

  @Override
  public int hashCode() {
    int result = occurredOn.hashCode();
    result = 31 * result + groupId.hashCode();
    result = 31 * result + oldName.hashCode();
    result = 31 * result + newName.hashCode();
    result = 31 * result + triggeredByUserId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "GroupProfileUpdated{" +
        "occurredOn=" + occurredOn +
        ", groupId='" + groupId + '\'' +
        ", oldName='" + oldName + '\'' +
        ", newName='" + newName + '\'' +
        ", triggeredByUserId='" + triggeredByUserId + '\'' +
        '}';
  }
}