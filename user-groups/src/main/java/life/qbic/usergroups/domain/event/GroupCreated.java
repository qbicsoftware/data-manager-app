package life.qbic.usergroups.domain.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.usergroups.domain.model.GroupType;

/**
 * <b>A new user group has been created.</b>
 *
 * <p>Published by the {@code GroupDomainService} after a group has been successfully stored.
 * Carries the group identity, its name, its type and the id of the creating user.
 *
 * @since 1.0.0
 */
public class GroupCreated extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 5012345678901234567L;

  @JsonProperty("groupId")
  private String groupId;
  @JsonProperty("name")
  private String name;
  @JsonProperty("type")
  private GroupType type;
  @JsonProperty("creatorUserId")
  private String creatorUserId;

  protected GroupCreated() {
    // for deserialization
  }

  /**
   * Creates a new {@link GroupCreated} event.
   *
   * @param groupId       the id of the created group
   * @param name          the name of the created group
   * @param type          the type of the created group
   * @param creatorUserId the user id of the creating user
   * @return the new event
   * @since 1.0.0
   */
  public static GroupCreated create(String groupId, String name, GroupType type,
      String creatorUserId) {
    return new GroupCreated(groupId, name, type, creatorUserId);
  }

  private GroupCreated(String groupId, String name, GroupType type, String creatorUserId) {
    this.groupId = groupId;
    this.name = name;
    this.type = type;
    this.creatorUserId = creatorUserId;
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

  @JsonGetter("creatorUserId")
  public String creatorUserId() {
    return creatorUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GroupCreated that)) {
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
    return creatorUserId.equals(that.creatorUserId);
  }

  @Override
  public int hashCode() {
    int result = occurredOn.hashCode();
    result = 31 * result + groupId.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + creatorUserId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "GroupCreated{" +
        "occurredOn=" + occurredOn +
        ", groupId='" + groupId + '\'' +
        ", name='" + name + '\'' +
        ", type=" + type +
        ", creatorUserId='" + creatorUserId + '\'' +
        '}';
  }
}