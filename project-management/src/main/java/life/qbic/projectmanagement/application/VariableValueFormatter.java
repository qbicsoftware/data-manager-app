package life.qbic.projectmanagement.application;

import static java.util.Objects.nonNull;

import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue;

public class VariableValueFormatter {

  private VariableValueFormatter() {}

  /**
   * Formats the {@link ExperimentalValue} to show the defined value with the unit if a unit is
   * present
   *
   * @param experimentalValue the {@link ExperimentalValue} with or without unit information
   * @return a formatted String which will contain a space between value and unit if a unit is
   * present
   */
  public static String format(ExperimentalValue experimentalValue) {
    return format(experimentalValue.value(), experimentalValue.unit().orElse(null));
  }

  public static String format(String value, String unit) {
    return value + (nonNull(unit) ? " " + unit : "");
  }
}
