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

  /**
   * Takes a {@link Condition} and transforms it into a String representation.
   * <p>
   * In its current implementation, the String representation results into a
   * <code>;</code>-separated concatenation of {@link VariableLevel}. See
   * {@link PropertyToString#variableLevel(VariableLevel)} for more details.
   * <p>
   * Example: a condition with the two variable levels <code>size: 20 cm</code> and <code>hue:
   * blue</code> will result in:
   * <p>
   * <code>size: 20cm; hue: blue</code>
   * <p>
   * The generalised form is: <p>
   * <code>[var name 1]: [level value 1] [unit 1];...; [var name N]: [level value N] [unit N];
   * </code>
   *
   * @param condition the condition object to transform
   * @return the String representation
   * @since 1.5.0
   */
  public static String condition(Condition condition) {
    Objects.requireNonNull(condition);
    List<String> stringValues = new ArrayList<>();
    condition.getVariableLevels()
        .forEach(variableLevel -> stringValues.add(variableLevel(variableLevel)));
    return String.join("; ", stringValues);
  }

  /**
   * Takes a {@link VariableLevel} and transforms it into a String representation.
   * <p>
   * The generalised form of the output can be described with:
   * <p>
   * <code>[name]: [value] [unit]</code>
   *
   * @param variableLevel the variable level to transform
   * @return the String representation of the variable level
   * @since 1.5.0
   */
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

  /**
   * Transforms an {@link OntologyTerm} to its String representation.
   * <p>
   * The String representation currently contains the term label and the OBO ID.
   * <p>
   * The generalised form can be described as:
   * <p>
   * <code>[label] [[obo ID]]</code>
   * <p>
   * So e.g. 'Homo sapiens [NCBITaxon:9606]'
   *
   * @param ontologyTerm the ontology term to transform
   * @return the String representation of the ontology term
   * @since 1.5.0
   */
  public static String ontologyTerm(OntologyTerm ontologyTerm) {
    Objects.requireNonNull(ontologyTerm);
    return ONTOLOGY_TERM.formatted(ontologyTerm.getLabel(), ontologyTerm.getOboId());
  }
}
