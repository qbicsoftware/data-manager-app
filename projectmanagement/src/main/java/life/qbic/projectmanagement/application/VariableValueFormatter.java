package life.qbic.projectmanagement.application;

import life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue;

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
    String optionalUnit = experimentalValue.unit().map(unit -> " " + unit).orElse("");
    return experimentalValue.value() + optionalUnit;
  }
}
