package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.Panel;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class ExperimentalVariablePanel extends Panel {

  @Serial
  private static final long serialVersionUID = -7164573505302036117L;
  private final transient ExperimentalVariable experimentalVariable;

  public ExperimentalVariablePanel(ExperimentalVariable experimentalVariable) {
    super();
    this.experimentalVariable = experimentalVariable;
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experimental-variable-panel");
    Span cardHeader = new Span();
    cardHeader.addClassName("header");

    cardHeader.add(title(experimentalVariable.name().value()));
    this.add(cardHeader);

    Div tagContainer = new Div();
    tagContainer.addClassName("tags");
    tagContainer.add(experimentalVariableLevels());
    this.add(tagContainer);
  }

  private Span title(String title) {
    Span cardTitle = new Span();
    cardTitle.setText(title);
    cardTitle.addClassName("title");
    return cardTitle;
  }

  private Div experimentalVariableLevels() {
    Div layout = new Div();
    List<Tag> tags = createTagsFromExperimentalVariableLevels(experimentalVariable.levels());
    tags.forEach(layout::add);
    return layout;
  }

  private List<Tag> createTagsFromExperimentalVariableLevels(List<VariableLevel> variableLevels) {
    String tagFormat = "%s %s"; // "<value> [<unit>]"
    return variableLevels.stream()
        .sorted(Comparator.comparing(variable -> variable.variableName().value()))
        .map(variableLevel -> new Tag(
            tagFormat.formatted(variableLevel.experimentalValue().value(),
                variableLevel.experimentalValue().unit().orElse("").trim()))).toList();
  }

  public ExperimentalVariable experimentalVariable() {
    return this.experimentalVariable;
  }

}
