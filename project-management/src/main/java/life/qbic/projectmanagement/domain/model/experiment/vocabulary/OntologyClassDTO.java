package life.qbic.projectmanagement.domain.model.experiment.vocabulary;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class OntologyClassDTO {

  String ontology;

  String ontologyVersion;

  String ontologyIri;

  String termLabel;

  String name;

  String description;

  String classIri;

  public OntologyClassDTO() {
  }

  public OntologyClassDTO(String ontology, String ontologyVersion, String ontologyIri,
      String label, String name, String description, String classIri) {
    this.ontology = ontology;
    this.ontologyVersion = ontologyVersion;
    this.ontologyIri = ontologyIri;
    this.termLabel = label;
    this.name = name;
    this.description = description;
    this.classIri = classIri;
  }

  /*
  public static OntologyClassDTO from(OntologyClassEntityTest lookupEntity) {
    return new OntologyClassDTO(lookupEntity.getOntology(), lookupEntity.getOntologyVersion(),
        lookupEntity.getOntologyIri(), lookupEntity.getLabel(), lookupEntity.getName(),
        lookupEntity.getDescription(), lookupEntity.getClassIri());
  }*/

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
    return termLabel;
  }

  public void setLabel(String termLabel) {
    this.termLabel = termLabel;
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
        that.ontologyIri) && Objects.equals(termLabel, that.termLabel) && Objects.equals(
        name, that.name) && Objects.equals(description, that.description)
        && Objects.equals(classIri, that.classIri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ontology, ontologyVersion, ontologyIri, termLabel, name, description, classIri);
  }

}