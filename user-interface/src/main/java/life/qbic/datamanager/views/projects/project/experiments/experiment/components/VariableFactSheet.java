package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import java.util.Comparator;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;

/**
 * A fact sheet displaying information on an experimental variable.
 *
 * @since 1.0.0
 */
public class VariableFactSheet extends Div {

  private final ExperimentalVariable experimentalVariable;
  private Div header;
  private Div content;
  private UnorderedList variableLevels;

  public VariableFactSheet(ExperimentalVariable variable) {
    this.experimentalVariable = variable;
    addClassName("experimental-variables-fact-sheet");
    initHeader();
    initContent();
    fillHeader();
    fillContent();
    add(header, content);
  }

  private void initContent() {
    var content = new Div();
    content.addClassName("variable-values");
    variableLevels = new UnorderedList();
    content.add(variableLevels);
    this.content = content;
  }

  private void initHeader() {
    var header = new Div();
    header.addClassName("variable-header");
    this.header = header;
  }

  private void fillContent() {
    variableLevels.removeAll();
    ListItem[] listItems = experimentalVariable.levels().stream()
        .map(VariableLevel::experimentalValue)
        .map(ExperimentalValue::value)
        .sorted(new StringOrNumberComparator())
        .map(ListItem::new)
        .toArray(ListItem[]::new);
    variableLevels.add(listItems);
  }

  private void fillHeader() {
    header.removeAll();
    header.add(new Span(formatHeaderText()));
  }

  private String formatHeaderText() {
    var unit = experimentalVariable.levels().get(0).experimentalValue().unit();
    return unit.map(s -> experimentalVariable.name().value() + " [" + s + "]")
        .orElseGet(() -> experimentalVariable.name().value());
  }

  private static class StringOrNumberComparator implements Comparator<String> {

    public StringOrNumberComparator() {
    }

    @Override
    public int compare(String o1, String o2) {
      if (bothAreNumbers(o1, o2)) {
        return compareNumbers(Double.parseDouble(o1), Double.parseDouble(o2));
      }
      return o1.compareTo(o2);
    }

    public boolean bothAreNumbers(String o1, String o2) {
      try {
        Double.parseDouble(o1);
        Double.parseDouble(o2);
      } catch (NumberFormatException ignore) {
        return false;
      }
      return true;
    }

    private int compareNumbers(Double o1, Double o2) {
      return (int) (o1 - o2);
    }
  }
}
