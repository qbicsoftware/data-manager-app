package life.qbic.projectmanagement.infrastructure.ontology;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * <b>Tib Term</b>
 * <p>
 * Class representing the TIB Terminology JSON response object for a term.
 *
 * @since 1.4.0
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TibTerm tibTerm = (TibTerm) o;
    return Objects.equals(id, tibTerm.id) && Objects.equals(iri, tibTerm.iri)
        && Objects.equals(shortForm, tibTerm.shortForm) && Objects.equals(oboId,
        tibTerm.oboId) && Objects.equals(label, tibTerm.label) && Objects.equals(
        ontologyName, tibTerm.ontologyName) && Objects.equals(ontologyPrefix,
        tibTerm.ontologyPrefix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, iri, shortForm, oboId, label, ontologyName, ontologyPrefix);
  }

  @Override
  public String toString() {
    return "TibTerm{" +
        "id='" + id + '\'' +
        ", iri='" + iri + '\'' +
        ", shortForm='" + shortForm + '\'' +
        ", oboId='" + oboId + '\'' +
        ", label='" + label + '\'' +
        ", ontologyName='" + ontologyName + '\'' +
        ", ontologyPrefix='" + ontologyPrefix + '\'' +
        '}';
  }
}
