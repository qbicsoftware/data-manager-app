package life.qbic.projectmanagement.infrastructure.organisations;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RorEntryV2 implements RorEntry {

  @JsonProperty("id")
  String id;

  @JsonProperty("names")
  List<OrganisationName> names;

  public static class OrganisationName {

    @JsonProperty("lang")
    String language;

    @JsonProperty("types")
    List<String> types;

    @JsonProperty("value")
    String value;

    public String getLanguage() {
      return language;
    }

    public List<String> getTypes() {
      return types;
    }

    public String getValue() {
      return value;
    }
  }

  public String getId() {
    return id;
  }

  @Override
  public String getDisplayedName() {
    return names.stream().filter(name -> name.getTypes().contains("ror_display")).findFirst()
        .map(OrganisationName::getValue)
        .orElseThrow();
  }
}
