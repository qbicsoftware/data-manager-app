package life.qbic.projectmanagement.domain.model.experiment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * <b>Experimental Group</b>
 * <p>
 * An {@link ExperimentalGroup} in a comparative experiment, is the group being tested for a
 * reaction to a change in independent variables.
 *
 * @since 1.0.0
 */

@Entity(name = "experimental_group")
public class ExperimentalGroup {

  private String name;
  private Condition condition;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long experimentalGroupId;
  private int sampleSize;

  private ExperimentalGroup(String name, Condition condition, int sampleSize) {
    this.name = name;
    this.condition = condition;
    this.sampleSize = sampleSize;
  }

  protected ExperimentalGroup() {
    // Please use the create method. This is needed for JPA
  }

  /**
   * Creates a new instance of an experimental group object.
   *
   * @param name       an optional name of the experimental group
   * @param condition  the condition the experimental group represents
   * @param sampleSize the number of samples in this experimental group
   * @return the experimental group
   * @since 1.0.0
   */
  public static ExperimentalGroup create(String name, Condition condition, int sampleSize) {
    Objects.requireNonNull(condition);
    if (sampleSize < 1) {
      // Admitting not very meaningful to allow for sample size of 1 and 2
      // However we leave it up to the project manager to make that decision
      throw new IllegalArgumentException(
          "The number of biological replicates must be at least one");
    }
    return new ExperimentalGroup(name, condition, sampleSize);
  }

  public Condition condition() {
    return this.condition;
  }

  public int sampleSize() {
    return this.sampleSize;
  }
  public String name() {
    return name;
  }

  public void setCondition(Condition condition) {
    this.condition = condition;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSampleSize(int sampleSize) {
    this.sampleSize = sampleSize;
  }
  public long id() {
    return this.experimentalGroupId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(experimentalGroupId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    //It's not possible to compare an experimentalGroup entity if it has no id
    if (this.experimentalGroupId == null) {
      return false;
    }
    return this.experimentalGroupId.equals(((ExperimentalGroup) o).experimentalGroupId);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ExperimentalGroup.class.getSimpleName() + "[", "]")
        .add("name=" + name)
        .add("condition=" + condition)
        .add("experimentalGroupId=" + experimentalGroupId)
        .add("sampleSize=" + sampleSize)
        .toString();
  }
}
