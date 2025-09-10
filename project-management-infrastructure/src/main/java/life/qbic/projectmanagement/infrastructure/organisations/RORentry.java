package life.qbic.projectmanagement.infrastructure.organisations;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <b>Research Organisation Registry Entry</b>
 *
 * <p>A ROR entry that is returned by the ROR API.</p>
 *
 * @since 1.0.0
 */
public class RORentry {

  @JsonProperty("id")
  String id;

  @JsonProperty("name")
  String name;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    if (name == null) {
      name = "";
    }
    this.name = name.trim();
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
