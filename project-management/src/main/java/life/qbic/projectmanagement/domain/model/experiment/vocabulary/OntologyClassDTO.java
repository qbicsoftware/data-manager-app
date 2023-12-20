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

  private String ontologyAbbreviation;
  private String ontologyVersion;
  private String ontologyIri;
  private String classLabel;
  private String className;
  private String description;
  private String classIri;

  public OntologyClassDTO() {
  }

  /**
   * @param ontologyAbbreviation - the abbreviation of the ontology a class/term belongs
   *                             to
   * @param ontologyVersion      - the version of the ontology
   * @param ontologyIri          - the iri of this ontology (e.g. link to the owl)
   * @param classLabel                - a humanly readable classLabel for the term
   * @param className                 - the identifier unique for this ontology and term
   *                             (e.g. NCBITaxon_9606)
   * @param description          - an optional description of the term
   * @param classIri             - the iri where this specific class is found/described
   */
  public OntologyClassDTO(String ontologyAbbreviation, String ontologyVersion, String ontologyIri,
      String classLabel, String className, String description, String classIri) {
    this.ontologyAbbreviation = ontologyAbbreviation;
    this.ontologyVersion = ontologyVersion;
    this.ontologyIri = ontologyIri;
    this.classLabel = classLabel;
    this.className = className;
    this.description = description;
    this.classIri = classIri;
  }

  public static OntologyClassDTO from(OntologyClassEntity lookupEntity) {
    return new OntologyClassDTO(lookupEntity.getOntologyAbbreviation(),
        lookupEntity.getOntologyVersion(),
        lookupEntity.getOntologyIri(), lookupEntity.getLabel(), lookupEntity.getName(),
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

  public String getName() {
    return className;
  }

  public void setName(String name) {
    this.className = name;
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
    return "%s (%s)".formatted(className,
        Ontology.findOntologyByAbbreviation(ontologyAbbreviation).getName());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    OntologyClassDTO that = (OntologyClassDTO) object;

    if (!Objects.equals(ontologyAbbreviation, that.ontologyAbbreviation)) {
      return false;
    }
    if (!Objects.equals(ontologyVersion, that.ontologyVersion)) {
      return false;
    }
    if (!Objects.equals(ontologyIri, that.ontologyIri)) {
      return false;
    }
    if (!Objects.equals(classLabel, that.classLabel)) {
      return false;
    }
    if (!Objects.equals(className, that.className)) {
      return false;
    }
    if (!Objects.equals(description, that.description)) {
      return false;
    }
    return Objects.equals(classIri, that.classIri);
  }

  @Override
  public int hashCode() {
    int result = ontologyAbbreviation != null ? ontologyAbbreviation.hashCode() : 0;
    result = 31 * result + (ontologyVersion != null ? ontologyVersion.hashCode() : 0);
    result = 31 * result + (ontologyIri != null ? ontologyIri.hashCode() : 0);
    result = 31 * result + (classLabel != null ? classLabel.hashCode() : 0);
    result = 31 * result + (className != null ? className.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (classIri != null ? classIri.hashCode() : 0);
    return result;
  }
}
