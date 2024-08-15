package life.qbic.projectmanagement.infrastructure.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class TibTerm {

  @JsonProperty("id")
  String id;
  @JsonProperty("iri")
  String iri;
  @JsonProperty("short_form")
  String shortForm;
  @JsonProperty("obo_id")
  String oboId;
  @JsonProperty("label")
  String label;
  @JsonProperty("ontology_name")
  String ontologyName;
  @JsonProperty("ontology_prefix")
  String ontologyPrefix;

}
