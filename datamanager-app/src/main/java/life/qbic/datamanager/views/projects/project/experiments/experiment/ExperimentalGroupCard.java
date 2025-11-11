package life.qbic.datamanager.views.projects.project.experiments.experiment;


import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;

/**
 * <b>Experimental Group Card</b>
 * <p>
 * An experimental group card can be used to display content of {@link ExperimentalGroup} and
 * provide interaction, such as edit and deletion.
 *
 * @since 1.0.0
 */
public class ExperimentalGroupCard extends Card {

  @Serial
  private static final long serialVersionUID = -8400631799486647200L;
  private final transient ExperimentalGroup experimentalGroup;

  public ExperimentalGroupCard(ExperimentalGroup experimentalGroup) {
    super();
    this.experimentalGroup = experimentalGroup;
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experimental-group");

    Div cardHeader = new Div();
    cardHeader.addClassName("header");

    String title = experimentalGroup().name().isBlank() ? "Experimental Group" : experimentalGroup.name();

    cardHeader.add(title(title));
    this.add(cardHeader);

    Div cardContent = new Div();
    cardContent.add(condition());
    cardContent.add(sampleSize());
    cardContent.addClassName("content");
    this.add(cardContent);
  }


  private Span title(String value) {
    Span cardTitle = new Span();
    cardTitle.setText(value);
    cardTitle.addClassName("card-title");
    return cardTitle;
  }

  private Div condition() {
    var variableLevels = experimentalGroup.condition().getVariableLevels();
    Div tagLayout = new Div();
    tagLayout.addClassName("tag-collection");
    String tagFormat = "%s %s"; // "<value> [<unit>]"
    List<Tag> tags = variableLevels.stream()
        .sorted(Comparator.comparing(variable -> variable.variableName().value()))
        .map(variableLevel -> new Tag(tagFormat.formatted(variableLevel.experimentalValue().value(),
            variableLevel.experimentalValue().unit().orElse("").trim()))).toList();
    tags.forEach(tagLayout::add);
    return tagLayout;
  }

  private Span sampleSize() {
    Span span = new Span();
    span.add("Replicates: ");
    span.add(String.valueOf(experimentalGroup.sampleSize()));
    return span;
  }


  public long groupId() {
    return this.experimentalGroup.id();
  }

  public ExperimentalGroup experimentalGroup() {
    return this.experimentalGroup;
  }

}
