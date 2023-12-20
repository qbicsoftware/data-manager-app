package life.qbic.projectmanagement.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "ontology_classes")
public class OntologyClassEntity {

  String ontologyAbbreviation;

  String ontologyVersion;

  String ontologyIri;

  String label;

  String name;

  @Column(length = 2000)
  String description;

  String classIri;

  @Id
  @GeneratedValue
  private Long id;

  public OntologyClassEntity() {
  }

  public OntologyClassEntity(String ontologyAbbreviation, String ontologyVersion,
      String ontologyIri,
      String label, String name, String description, String classIri) {
    this.ontologyAbbreviation = ontologyAbbreviation;
    this.ontologyVersion = ontologyVersion;
    this.ontologyIri = ontologyIri;
    this.label = label;
    this.name = name;
    this.description = description;
    this.classIri = classIri;
  }

  public String getOntologyAbbreviation() {
    return ontologyAbbreviation;
  }

  public void setOntologyAbbreviation(String ontology) {
    this.ontologyAbbreviation = ontology;
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

  public void setLabel(String label) {
    this.label = label;
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
    OntologyClassEntity that = (OntologyClassEntity) o;
    return Objects.equals(ontologyAbbreviation, that.ontologyAbbreviation) && Objects.equals(
        ontologyVersion, that.ontologyVersion) && Objects.equals(ontologyIri,
        that.ontologyIri) && Objects.equals(label, that.label) && Objects.equals(
        name, that.name) && Objects.equals(description, that.description)
        && Objects.equals(classIri, that.classIri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ontologyAbbreviation, ontologyVersion, ontologyIri, label, name,
        description, classIri);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
