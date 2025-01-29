package life.qbic.datamanager.files.structure.rocrate;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A schema.org <a href="https://schema.org/ResearchProject">ResearchProject</a> representation in Java.
 *
 * @since 1.6.0.
 */
public class ResearchProject {

  @JsonProperty(value = "@type")
  private final String type = "ResearchProject";

  @JsonProperty(value = "name")
  private String name;

  @JsonProperty(value = "identifier")
  private String identifier;

  @JsonProperty(value = "description")
  private String description;

  @JsonProperty(value = "contactPoint")
  private List<ContactPoint> contactPoint;

  public static ResearchProject from(String name, String identifier, String description, List<ContactPoint> contactPoint) {
    ResearchProject project = new ResearchProject();
    project.name = name;
    project.identifier = identifier;
    project.description = description;
    project.contactPoint = contactPoint.stream().toList();
    return project;
  }

  public String name() {
    return name;
  }

  public String identifier() {
    return identifier;
  }

  public String description() {
    return description;
  }

  public List<ContactPoint> contactPoint() {
    return contactPoint.stream().toList();
  }

}
