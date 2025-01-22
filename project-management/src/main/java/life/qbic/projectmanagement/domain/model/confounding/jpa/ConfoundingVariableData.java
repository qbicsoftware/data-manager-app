package life.qbic.projectmanagement.domain.model.confounding.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Data describing a confounding variable in the database
 *
 * @since 1.8.0
 */
@Entity
@Table(name = "experiments_datamanager_confounding_variables")
public class ConfoundingVariableData {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;
  @Column(name = "experimentId", nullable = false)
  private String experimentId;
  @Column(name = "name", nullable = false)
  private String name;


  protected ConfoundingVariableData() {
  }

  public ConfoundingVariableData(Long id, String experimentId, String name) {
    this.id = id;
    this.experimentId = experimentId;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  protected void setId(Long id) {
    this.id = id;
  }

  public String getExperimentId() {
    return experimentId;
  }

  protected void setExperimentId(String experimentId) {
    this.experimentId = experimentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConfoundingVariableData that)) {
      return false;
    }

    return Objects.equals(id, that.id) && Objects.equals(experimentId, that.experimentId)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(experimentId);
    result = 31 * result + Objects.hashCode(name);
    return result;
  }
}
