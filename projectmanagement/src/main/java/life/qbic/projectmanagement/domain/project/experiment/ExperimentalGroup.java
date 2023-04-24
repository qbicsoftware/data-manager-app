package life.qbic.projectmanagement.domain.project.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.*;

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

  private Condition condition;

  private int sampleSize;
  @Id
  @GeneratedValue
  private Long experimentalGroupId;

  @OneToMany
  @JoinColumn(name = "experimentalGroupId")
  private List<BiologicalReplicate> biologicalReplicates;

  private ExperimentalGroup(Condition condition, int sampleSize) {
    this.condition = condition;
    this.sampleSize = sampleSize;
    this.biologicalReplicates = new ArrayList<>(sampleSize);
    generateBiologicalReplicates(sampleSize);
  }

  private void generateBiologicalReplicates(int amount) {
    for (int counter=1; counter <= amount; counter++) {
      final BiologicalReplicate replicate = BiologicalReplicate.create();
      biologicalReplicates.add(replicate);
    }
  }

  protected ExperimentalGroup() {
    // Please use the create method. This is needed for JPA
  }

  /**
   * Creates a new instance of an experimental group object.
   *
   * @param condition the condition the experimental group represents
   * @param sampleSize the number of samples in this experimental group
   * @return the experimental group
   * @since 1.0.0
   */
  public static ExperimentalGroup create(Condition condition, int sampleSize) {
    Objects.requireNonNull(condition);
    if (sampleSize < 1) {
      // Admitting not very meaningful to allow for sample size of 1 and 2
      // However we leave it up to the project manager to make that decision
      throw new IllegalArgumentException("The number of biological replicates must be at least one");
    }
    return new ExperimentalGroup(condition, sampleSize);
  }

  public Condition condition() {
    return this.condition;
  }

  public int sampleSize() {
    return this.sampleSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExperimentalGroup that = (ExperimentalGroup) o;
    return sampleSize == that.sampleSize && Objects.equals(condition, that.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, sampleSize);
  }

}
