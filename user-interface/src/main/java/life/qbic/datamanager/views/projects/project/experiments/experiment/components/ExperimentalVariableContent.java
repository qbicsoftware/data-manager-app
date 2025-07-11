package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import java.util.List;

/**
 * @param name   the name of the variable
 * @param unit   the unit of the variable levels
 * @param levels the variable levels that are assigned to the variable
 */
public record ExperimentalVariableContent(String name, String unit, List<String> levels) {
//
//  static ExperimentalVariableContent from(final ExperimentalVariableRow layout) {
//    var information = layout.getOptionalValue()
//        .orElseThrow(() -> new IllegalArgumentException("No variable information present"));
//    return new ExperimentalVariableContent(information.variableName(), information.unit(), information.levels());
//  }

}
