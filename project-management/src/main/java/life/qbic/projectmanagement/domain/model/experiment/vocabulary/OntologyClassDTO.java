package life.qbic.projectmanagement.domain.model.experiment.vocabulary;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.experiment.repository.jpa.OntologyClassAttributeConverter;

/**
 * Describes Ontology Class objects and is used to store and display species, specimen, analyte etc.
 * when creating or editing experiments and samples. Other than {@link OntologyClassEntity}, which
 * is used for lookup in the non-persistent ontology table, OntologyClassDTO objects
 * with ontology versions are stored persistently with experiments and samples. Storage
 * is facilitated by {@link OntologyClassAttributeConverter}.
 */
public class OntologyClassDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1459801951948902353L;

  String ontology;

  String ontologyVersion;

  String ontologyIri;

  String label;

  String name;

  String description;

  String classIri;

  public OntologyClassDTO() {
  }

  /**
   * @param ontology - the abbreviation of the ontology a class/term belongs
   *                             to
   * @param ontologyVersion      - the version of the ontology
   * @param ontologyIri          - the iri of this ontology (e.g. link to the owl)
   * @param label                - a humanly readable label for the term
   * @param name                 - the identifier unique for this ontology and term
   *                             (e.g. NCBITaxon_9606)
   * @param description          - an optional description of the term
   * @param classIri             - the iri where this specific class is found/described
   */
  public OntologyClassDTO(String ontology, String ontologyVersion, String ontologyIri,
      String label, String name, String description, String classIri) {
    this.ontology = ontology;
    this.ontologyVersion = ontologyVersion;
    this.ontologyIri = ontologyIri;
    this.label = label;
    this.name = name;
    this.description = description;
    this.classIri = classIri;
  }

  public static OntologyClassDTO from(OntologyClassEntity lookupEntity) {
    return new OntologyClassDTO(lookupEntity.getOntology(), lookupEntity.getOntologyVersion(),
        lookupEntity.getOntologyIri(), lookupEntity.getLabel(), lookupEntity.getName(),
        lookupEntity.getDescription(), lookupEntity.getClassIri());
  }

  public String getOntology() {
    return ontology;
  }

  public void setOntology(String ontology) {
    this.ontology = ontology;
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
    return label;
  }

  public void setLabel(String termLabel) {
    this.label = termLabel;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
    return "%s (%s)".formatted(name,
        Ontology.findOntologyByAbbreviation(ontology).getName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OntologyClassDTO that = (OntologyClassDTO) o;
    return Objects.equals(ontology, that.ontology) && Objects.equals(
        ontologyVersion, that.ontologyVersion) && Objects.equals(ontologyIri,
        that.ontologyIri) && Objects.equals(label, that.label) && Objects.equals(
        name, that.name) && Objects.equals(description, that.description)
        && Objects.equals(classIri, that.classIri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ontology, ontologyVersion, ontologyIri, label, name,
        description, classIri);
  }

}