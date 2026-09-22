package life.qbic.usergroups.domain.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.usergroups.domain.model.GroupType;

/**
 * <b>A user group has been dissolved.</b>
 *
 * <p>Published by the {@code GroupDomainService} after an ad-hoc group's last membership was
 * removed and the group was dissolved (soft dissolve: the group row is kept, its status becomes
 * {@code DISSOLVED} and all memberships are purged).
 *
 * @since 1.0.0
 */
public class GroupDissolved extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 5123456789012345678L;

  @JsonProperty("groupId")
  private String groupId;
  @JsonProperty("name")
  private String name;
  @JsonProperty("type")
  private GroupType type;
  @JsonProperty("triggeredByUserId")
  private String triggeredByUserId;

  protected GroupDissolved() {
    // for deserialization
  }

  /**
   * Creates a new {@link GroupDissolved} event.
   *
   * @param groupId           the id of the dissolved group
   * @param name              the name of the dissolved group
   * @param type              the type of the dissolved group
   * @param triggeredByUserId the user id of the user whose removal dissolved the group
   * @return the new event
   * @since 1.0.0
   */
  public static GroupDissolved create(String groupId, String name, GroupType type,
      String triggeredByUserId) {
    return new GroupDissolved(groupId, name, type, triggeredByUserId);
  }

  private GroupDissolved(String groupId, String name, GroupType type, String triggeredByUserId) {
    this.groupId = groupId;
    this.name = name;
    this.type = type;
    this.triggeredByUserId = triggeredByUserId;
  }

  @JsonGetter("groupId")
  public String groupId() {
    return groupId;
  }

  @JsonGetter("name")
  public String groupName() {
    return name;
  }

  @JsonGetter("type")
  public GroupType groupType() {
    return type;
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
    if (!(o instanceof GroupDissolved that)) {
      return false;
    }
    if (!occurredOn.equals(that.occurredOn)) {
      return false;
    }
    if (!groupId.equals(that.groupId)) {
      return false;
    }
    if (!name.equals(that.name)) {
      return false;
    }
    if (type != that.type) {
      return false;
    }
    return triggeredByUserId.equals(that.triggeredByUserId);
  }

  @Override
  public int hashCode() {
    int result = occurredOn.hashCode();
    result = 31 * result + groupId.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + triggeredByUserId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "GroupDissolved{" +
        "occurredOn=" + occurredOn +
        ", groupId='" + groupId + '\'' +
        ", name='" + name + '\'' +
        ", type=" + type +
        ", triggeredByUserId='" + triggeredByUserId + '\'' +
        '}';
  }
}