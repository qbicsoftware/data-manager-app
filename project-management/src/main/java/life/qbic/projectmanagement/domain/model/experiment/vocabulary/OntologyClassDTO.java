package life.qbic.projectmanagement.domain.model.experiment.vocabulary;

import java.util.Objects;
import life.qbic.projectmanagement.application.OntologyClassEntity;

public class OntologyClassDTO {

  String ontology;

  String ontologyVersion;

  String ontologyIri;

  String label;

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
    return Objects.hash(ontology, ontologyVersion, ontologyIri, label, name, description, classIri);
  }

}