package life.qbic.datamanager.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A schema.org <a href="https://schema.org/ResearchProject">ResearchProject</a> representation in Java.
 *
 * @since 1.6.0.
 */
public class ResearchProject {

  @JsonProperty(value = "@type")
  private String type = "ResearchProject";

  @JsonProperty(value = "name")
  private String name;

  @JsonProperty(value = "identifier")
  private String identifier;

  @JsonProperty(value = "description")
  private String description;

  public static ResearchProject from(String name, String identifier, String description) {
    ResearchProject project = new ResearchProject();
    project.name = name;
    project.identifier = identifier;
    project.description = description;
    return project;
  }

  public String name() {
    return name;
  }

  public String identifier() {
    return identifier;
  }

}
