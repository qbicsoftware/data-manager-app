package life.qbic.projectmanagement.domain.model.confounding.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Data describing a confounding variable level in the database
 *
 * @since 1.8.0
 */
@Entity
@Table(name = "confounding_variable_levels")
public class ConfoundingVariableLevelData {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;
  @Column(name = "variableId", nullable = false)
  private long variableId;
  @Column(name = "sampleId", nullable = false)
  private String sampleId;
  @Column(name = "value")
  private String value;

  protected ConfoundingVariableLevelData() {
  }

  public ConfoundingVariableLevelData(Long id, long variableId, String sampleId, String value) {
    this.id = id;
    this.variableId = variableId;
    this.sampleId = sampleId;
    this.value = value;
  }

  public long getId() {
    return id;
  }

  protected void setId(long id) {
    this.id = id;
  }

  public long getVariableId() {
    return variableId;
  }

  protected void setVariableId(long variableId) {
    this.variableId = variableId;
  }

  public String getSampleId() {
    return sampleId;
  }

  protected void setSampleId(String sampleId) {
    this.sampleId = sampleId;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
