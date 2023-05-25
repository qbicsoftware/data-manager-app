package life.qbic.projectmanagement.domain.project.experiment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
  @JoinColumn(name = "experimentalGroupId")
  private List<BiologicalReplicate> biologicalReplicates;
  private Condition condition;
  @Id
  @GeneratedValue
  private Long experimentalGroupId;
  private int sampleSize;

  private ExperimentalGroup(Condition condition, int sampleSize,
      Collection<BiologicalReplicate> biologicalReplicates) {
    this.condition = condition;
    this.sampleSize = sampleSize;
    this.biologicalReplicates = biologicalReplicates.stream().toList();
  }

  protected ExperimentalGroup() {
    // Please use the create method. This is needed for JPA
  }

  @PostLoad
  private void loadReplicates() {
    this.biologicalReplicates.size();
  }

  /**
   * Creates a new instance of an experimental group object.
   *
   * @param condition  the condition the experimental group represents
   * @param sampleSize the number of samples in this experimental group
   * @return the experimental group
   * @since 1.0.0
   */
  public static ExperimentalGroup create(Condition condition, int sampleSize) {
    Objects.requireNonNull(condition);
    if (sampleSize < 1) {
      // Admitting not very meaningful to allow for sample size of 1 and 2
      // However we leave it up to the project manager to make that decision
      throw new IllegalArgumentException(
          "The number of biological replicates must be at least one");
    }
    return new ExperimentalGroup(condition, sampleSize, createBiologicalReplicates(sampleSize));
  }

  private static List<BiologicalReplicate> createBiologicalReplicates(int amount) {
    List<BiologicalReplicate> replicates = new ArrayList<>(amount);
    BiologicalReplicate.resetReplicateCounter();
    for (int i = 1; i <= amount; i++) {
      replicates.add(BiologicalReplicate.create());
    }
    return replicates;
  }

  public Condition condition() {
    return this.condition;
  }

  public int sampleSize() {
    return this.sampleSize;
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

}
