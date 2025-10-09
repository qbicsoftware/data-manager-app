package life.qbic.datamanager.views.projects.project.experiments.experiment;


import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.List;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.projectmanagement.application.VariableValueFormatter;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariable;

/**
 * <b>Experimental Variable Card</b>
 * <p>
 * An experimental variable card can be used to display content of {@link ExperimentalVariable} and
 * provide interaction, such as edit and deletion.
 *
 * @since 1.0.0
 */
public class ExperimentalVariableCard extends Card {

  @Serial
  private static final long serialVersionUID = -3801182379812377200L;
  private final transient ExperimentalVariable experimentalVariable;

  public ExperimentalVariableCard(ExperimentalVariable experimentalVariable) {
    super();
    this.experimentalVariable = experimentalVariable;
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experimental-group");

    Div cardHeader = new Div();
    cardHeader.addClassName("header");

    cardHeader.add(title(experimentalVariable.name()));
    this.add(cardHeader);

    Div cardContent = new Div();
    cardContent.add(levels());
    cardContent.addClassName("content");
    this.add(cardContent);
  }


  private Span title(String value) {
    Span cardTitle = new Span();
    cardTitle.setText(value);
    cardTitle.addClassName("card-title");
    return cardTitle;
  }

  private Div levels() {
    var variableLevels = experimentalVariable.levels();
    Div tagLayout = new Div();
    tagLayout.addClassName("tag-collection");
    List<Tag> tags = variableLevels.stream()
        .map(level -> VariableValueFormatter.format(level, experimentalVariable.unit()))
        .map(Tag::new)
        .toList();
    tags.forEach(tagLayout::add);
    return tagLayout;
  }

  public String variableName() {
    return this.experimentalVariable.name();
  }
}
