package life.qbic.projectmanagement.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.domain.model.experiment.repository.jpa.OntologyClassAttributeConverter;

/**
 * Describes Ontology Class objects and is used to store and display species, specimen, analyte etc.
 * when creating or editing experiments and samples. Other than {@link OntologyClass}, which is used
 * for lookup in the non-persistent ontology table, OntologyClassDTO objects with ontology versions
 * are stored persistently with experiments and samples. Storage is facilitated by
 * {@link OntologyClassAttributeConverter}.
 */
public class OntologyTerm implements Serializable {

  @Serial
  private static final long serialVersionUID = 1459801951948902353L;

  @JsonProperty("ontology")
  private String ontologyAbbreviation;
  @JsonProperty("ontologyVersion")
  private String ontologyVersion;
  @JsonProperty("ontologyIri")
  private String ontologyIri;
  @JsonProperty("label")
  private String classLabel;
  @JsonProperty("name")
  private String oboId;
  @JsonProperty("description")
  private String description;
  @JsonProperty("classIri")
  private String classIri;
  @JsonProperty("oboId")
  private OboId realOboId;

  public OntologyTerm() {
  }

  /**
   * @param ontologyAbbreviation - the abbreviation of the ontology a class/term belongs to
   * @param ontologyVersion      - the version of the ontology
   * @param ontologyIri          - the iri of this ontology (e.g. link to the owl)
   * @param classLabel           - a humanly readable classLabel for the term
   * @param oboId                - the identifier unique for this ontology and term (e.g.
   *                             NCBITaxon:9606 [OBO ID])
   * @param description          - an optional description of the term
   * @param classIri             - the iri where this specific class is found/described
   */
  public OntologyTerm(String ontologyAbbreviation, String ontologyVersion, String ontologyIri,
      String classLabel, String oboId, String description, String classIri) {
    this.ontologyAbbreviation = ontologyAbbreviation;
    this.ontologyVersion = ontologyVersion;
    this.ontologyIri = ontologyIri;
    this.classLabel = classLabel;
    this.oboId = oboId;
    this.description = description;
    this.classIri = classIri;
  }

  public static OntologyTerm from(OntologyClass lookupEntity) {
    return new OntologyTerm(lookupEntity.getOntologyAbbreviation(),
        lookupEntity.getOntologyVersion(),
        lookupEntity.getOntologyIri(), lookupEntity.getClassLabel(), lookupEntity.getCurie(),
        lookupEntity.getDescription(), lookupEntity.getClassIri());
  }

  public String getOntologyAbbreviation() {
    return ontologyAbbreviation;
  }

  public void setOntologyAbbreviation(String ontologyAbbreviation) {
    this.ontologyAbbreviation = ontologyAbbreviation;
  }

  public String getOntologyVersion() {
    return ontologyVersion;
  }

  public void setOntologyVersion(String ontologyVersion) {
    this.ontologyVersion = ontologyVersion;
  }

  public String getOntologyIri() {
    return ontologyIri;
  }

  public void setOntologyIri(String ontologyIri) {
    this.ontologyIri = ontologyIri;
  }

  public String getLabel() {
    return classLabel;
  }

  public void setLabel(String termLabel) {
    this.classLabel = termLabel;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Deprecated(since = "1.10.0", forRemoval = true)
  public String getOboId() {
    return oboId;
  }

  public OboId oboId() {
    // Legacy support for CURIE specification violation
    return Objects.requireNonNullElseGet(realOboId, () -> OboId.parse(oboId, "_"));
  }

  public void setOboId(OboId oboId) {
    this.realOboId = Objects.requireNonNull(oboId);
  }

  @Deprecated(since = "1.10.0", forRemoval = true)
  public void setOboId(String oboId) {
    this.oboId = oboId;
  }

  public String getClassIri() {
    return classIri;
  }

  public void setClassIri(String classIri) {
    this.classIri = classIri;
  }

  /**
   * Returns a formatted String representing the name and ontology of the OntologyClassDTO. The
   * format is "%s (%s)", where the first placeholder is the name and the second placeholder is the
   * name of the ontology.
   * <p>
   * For example: "PO_0000003 (Plant Ontology)" for `whole plant`
   *
   * @return a formatted String representing the name and ontology
   */
  public String formatted() {
    return "%s (%s)".formatted(oboId,
        Ontology.findOntologyByAbbreviation(ontologyAbbreviation).getName());
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    OntologyTerm that = (OntologyTerm) other;
    return Objects.equals(classIri, that.classIri);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(classIri);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", OntologyTerm.class.getSimpleName() + "[", "]")
        .add("ontologyAbbreviation='" + ontologyAbbreviation + "'")
        .add("classLabel='" + classLabel + "'")
        .add("oboId='" + oboId + "'")
        .add("classIri='" + classIri + "'")
        .toString();
  }

  public record OboId(String idSpace, String localId) {
    static OboId parse(String id) {
      if (id == null || id.isEmpty()) {
        throw new IllegalArgumentException("OboId cannot be null or empty");
      }
      String[] parts = id.split(":");
      return new OboId(parts[0], parts[1]);
    }

    static OboId parse(String id, String separator) {
      if (id == null || id.isEmpty()) {
        throw new IllegalArgumentException("OboId cannot be null or empty");
      }
      String[] parts = id.split(separator);
      return new OboId(parts[0], parts[1]);
    }

    public String toString() {
      return idSpace + ":" + localId;
    }
  }
}
