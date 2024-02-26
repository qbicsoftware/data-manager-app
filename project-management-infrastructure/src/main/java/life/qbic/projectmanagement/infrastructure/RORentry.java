package life.qbic.projectmanagement.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class RORentry {

  @JsonProperty("id")
  String id;

  @JsonProperty("name")
  String name;

  public void setName(String name) {
    if (name == null) {
      name = "";
    }
    this.name = name.trim();
  }

  public String getName() {
    return this.name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (id == null) {
      id = "";
    }
    this.id = id;
  }
}
