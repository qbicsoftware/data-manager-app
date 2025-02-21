package life.qbic.datamanager.views.general.confounding;

import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;

/**
 * Data record for use with {@link ConfoundingVariablesUserInput}
 *
 * @param variableReference the reference to the variable
 * @param name              the name of the variable
 */
public record ConfoundingVariable(VariableReference variableReference, String name) {

}
