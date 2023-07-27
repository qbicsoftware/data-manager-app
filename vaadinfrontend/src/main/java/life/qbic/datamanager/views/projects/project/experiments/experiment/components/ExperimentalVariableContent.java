package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import java.util.List;

/**
 * @param name   the name of the variable
 * @param unit   the unit of te variable levels
 * @param levels the variable levels that are assigned to the variable
 */
public record ExperimentalVariableContent(String name, String unit, List<String> levels) {

  static ExperimentalVariableContent from(final ExperimentalVariableRowLayout layout) {
    final String variableName = layout.getVariableName();
    final String unit = layout.getUnit();
    final List<String> levels = layout.getLevels();
    return new ExperimentalVariableContent(variableName, unit, levels);
  }

}
