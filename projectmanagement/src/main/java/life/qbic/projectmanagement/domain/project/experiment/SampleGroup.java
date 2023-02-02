package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;

/**
 * <b>Sample Group</b>
 * <p>
 * A {@link SampleGroup} can be defined via {@link ExperimentalDesign#createSampleGroup(String, int, Long)} and represent
 * a logical container of biological replicates of one condition in an experimental design.
 *
 * @since 1.0.0
 */
public class SampleGroup {

  private final String label;
  private final Condition condition;
  private final int biologicalReplicates;

  private SampleGroup(String label, Condition condition, int biologicalReplicates) {
    this.label = label;
    this.condition = condition;
    this.biologicalReplicates = biologicalReplicates;
  }

  /**
   * Creates a new instance of a sample group object.
   *
   * @param label a textual tag for the condition
   * @param condition the condition the sample group represents
   * @param biologicalReplicates the number of true biological replicates that are part of the sample
   *                             group
   * @return the sample group
   * @since 1.0.0
   */
  public static SampleGroup with(String label, Condition condition, int biologicalReplicates) {
    Objects.requireNonNull(condition);
    if (label.isBlank()) {
      throw new IllegalArgumentException("Name for sample group must not be blank");
    }
    if (biologicalReplicates < 1) {
      // Admitting not very meaningful to allow for replicates of 1 and 2
      // However we leave it up to the project manager to make that decision
      throw new IllegalArgumentException("The number of biological replicates must be at least one");
    }
    return new SampleGroup(label, condition, biologicalReplicates);
  }

  public Condition condition() {
    return this.condition;
  }

  public int biologicalReplicatesCount() {
    return this.biologicalReplicates;
  }

  public String label() {
    return this.label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SampleGroup that = (SampleGroup) o;
    return biologicalReplicates == that.biologicalReplicates && Objects.equals(label,
        that.label) && Objects.equals(condition, that.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, condition, biologicalReplicates);
  }
}
