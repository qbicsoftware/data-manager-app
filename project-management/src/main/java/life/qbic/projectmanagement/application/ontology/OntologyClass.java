package life.qbic.projectmanagement.application.ontology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "ontology_classes")
public class OntologyClass {

  @Column(name = "ontology")
  String ontologyAbbreviation;
  @Column(name = "ontologyVersion")
  String ontologyVersion;
  @Column(name = "ontologyIri")
  String ontologyIri;
  @Column(name = "label")
  String classLabel;
  @Column(name = "curi")
  String curi;
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
      String classLabel, String curi, String description, String classIri) {
    this.ontologyAbbreviation = ontologyAbbreviation;
    this.ontologyVersion = ontologyVersion;
    this.ontologyIri = ontologyIri;
    this.classLabel = classLabel;
    this.curi = curi;
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

  public String getCuri() {
    return curi;
  }

  public void setCuri(String name) {
    this.curi = name;
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
    return Objects.equals(ontologyAbbreviation, that.ontologyAbbreviation) && Objects.equals(
        ontologyVersion, that.ontologyVersion) && Objects.equals(ontologyIri,
        that.ontologyIri) && Objects.equals(classLabel, that.classLabel) && Objects.equals(
        curi, that.curi) && Objects.equals(description, that.description)
        && Objects.equals(classIri, that.classIri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ontologyAbbreviation, ontologyVersion, ontologyIri, classLabel, curi,
        description, classIri);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
