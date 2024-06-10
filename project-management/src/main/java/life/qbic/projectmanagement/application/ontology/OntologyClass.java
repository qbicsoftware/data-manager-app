package life.qbic.projectmanagement.application.ontology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "ontology_classes")
//NOTE: make sure to set the Engine to MyISAM and create a label and name fulltext index.
public class OntologyClass {

  @Column(name = "ontology")
  String ontologyAbbreviation;
  @Column(name = "ontologyVersion")
  String ontologyVersion;
  @Column(name = "ontologyIri")
  String ontologyIri;
  @Column(name = "label", length = 1024)
  String classLabel;
  @Column(name = "name")
  String curie;
  @Column(name = "description", length = 2000)
  String description;
  @Column(name = "classIri")
  String classIri;

  @Id
  @GeneratedValue
  private Long id;

  public OntologyClass() {
  }

  public OntologyClass(String ontologyAbbreviation, String ontologyVersion,
      String ontologyIri,
      String classLabel, String curie, String description, String classIri) {
    this.ontologyAbbreviation = ontologyAbbreviation;
    this.ontologyVersion = ontologyVersion;
    this.ontologyIri = ontologyIri;
    this.classLabel = classLabel;
    this.curie = curie;
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

  public String getClassLabel() {
    return classLabel;
  }

  public void setClassLabel(String label) {
    this.classLabel = label;
  }

  public String getCurie() {
    return curie;
  }

  public void setCurie(String name) {
    this.curie = name;
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
    OntologyClass that = (OntologyClass) o;
    return Objects.equals(classIri, that.classIri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classIri);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

}
