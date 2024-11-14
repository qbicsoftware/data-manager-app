package life.qbic.projectmanagement.application.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
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
public class PropertyConversion {

  private static final String CONDITION_VARIABLE_LEVEL_UNIT_TEMPLATE = "%s: %s %s"; // <variable name>: <value> [unit]

  private static final String CONDITION_VARIABLE_LEVEL_NO_UNIT_TEMPLATE = "%s: %s"; // <variable name>: <value>

  private static final String ONTOLOGY_TERM = "%s [%s]"; // <term label> [CURIE]

  private static final Pattern CURIE_PATTERN = Pattern.compile("\\[.*\\]");

  /**
   * Takes a {@link Condition} and transforms it into a String representation.
   * <p>
   * In its current implementation, the String representation results into a
   * <code>;</code>-separated concatenation of {@link VariableLevel}. See
   * {@link PropertyConversion#toString(VariableLevel)} for more details.
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
  public static String toString(Condition condition) {
    Objects.requireNonNull(condition);
    List<String> stringValues = new ArrayList<>();
    condition.getVariableLevels()
        .forEach(variableLevel -> stringValues.add(toString(variableLevel)));
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
  public static String toString(VariableLevel variableLevel) {
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
  public static String toString(OntologyTerm ontologyTerm) {
    Objects.requireNonNull(ontologyTerm);
    return ONTOLOGY_TERM.formatted(ontologyTerm.getLabel(), ontologyTerm.getOboId());
  }

  /**
   * Expects a String that has been created with {@link #toString(OntologyTerm)} and tries to
   * extract the CURIE of the ontology term.
   * <p>
   * The CURIE is expected to be surrounded with <code>[...]</code> squared brackets.
   *
   * @param term the term that might contain the CURIE (e.g. OBO ID)
   * @return an Optional that contains the found CURIE, or is empty else if none was found
   * @since 1.5.0
   */
  public static Optional<String> extractCURIE(String term) {
    return CURIE_PATTERN.matcher(term).results().map(MatchResult::group).findFirst()
        .map(PropertyConversion::sanitizeOntologyTerm);
  }

  private static String removeBrackets(String value) {
    return value.replace("[", "").replace("]", "");
  }

  private static String sanitizeOntologyTerm(String term) {
    return removeBrackets(term).trim();
  }

}
