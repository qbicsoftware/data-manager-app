package life.qbic.projectmanagement.domain.project.experiment;

import java.util.Objects;

/**
 * The label of a condition. This may be used by scientists, analysts and project managers to talk
 * about a condition or identify a condition in an experiment.
 */
public record ConditionLabel(String value) {

  public ConditionLabel {
    Objects.requireNonNull(value, "condition label must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("condition label must not be blank");
    }
  }

  public static ConditionLabel create(String dbData) {
    return new ConditionLabel(dbData);
  }
}
