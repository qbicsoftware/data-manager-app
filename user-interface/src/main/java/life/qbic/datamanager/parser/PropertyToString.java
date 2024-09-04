package life.qbic.datamanager.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;

/**
 * <b>Property to String conversion class</b>
 *
 * <p>Centralises logic to control the String representation of properties for the parsing
 * context.</p>
 *
 * @since 1.5.0
 */
public class PropertyToString {

  private static final String CONDITION_VARIABLE_LEVEL_UNIT_TEMPLATE = "%s: %s %s"; // <variable name>: <value> [unit]

  private static final String CONDITION_VARIABLE_LEVEL_NO_UNIT_TEMPLATE = "%s: %s"; // <variable name>: <value>

  private static final String ONTOLOGY_TERM = "%s [%s]"; // <term label> [CURIE]

  public static String condition(Condition condition) {
    Objects.requireNonNull(condition);
    List<String> stringValues = new ArrayList<>();
    condition.getVariableLevels().forEach(variableLevel -> stringValues.add(variableLevel(variableLevel)));
    return String.join("; ", stringValues);
  }

  public static String variableLevel(VariableLevel variableLevel) {
    Objects.requireNonNull(variableLevel);
    if (variableLevel.experimentalValue().unit().isPresent()) {
      return CONDITION_VARIABLE_LEVEL_UNIT_TEMPLATE.formatted(variableLevel.variableName().value(),
          variableLevel.experimentalValue().value(),
          variableLevel.experimentalValue().unit().get());
    } else {
      return CONDITION_VARIABLE_LEVEL_NO_UNIT_TEMPLATE.formatted(
          variableLevel.variableName().value(), variableLevel.experimentalValue().value());
    }
  }

  public static String ontologyTerm(OntologyTerm ontologyTerm) {
    Objects.requireNonNull(ontologyTerm);
    return ONTOLOGY_TERM.formatted(ontologyTerm.getLabel(), ontologyTerm.getOboId());
  }
}
