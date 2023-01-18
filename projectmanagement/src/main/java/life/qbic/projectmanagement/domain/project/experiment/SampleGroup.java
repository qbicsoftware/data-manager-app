package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
