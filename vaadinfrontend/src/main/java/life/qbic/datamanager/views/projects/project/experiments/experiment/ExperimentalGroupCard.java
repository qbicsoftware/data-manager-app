package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.Card;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;

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
  private final List<ComponentEventListener<ExperimentalGroupDeletionEvent>> listenersDeletionEvent;

  public ExperimentalGroupCard(ExperimentalGroup experimentalGroup) {
    super();
    this.experimentalGroup = experimentalGroup;
    this.listenersDeletionEvent = new ArrayList<>();
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experimental-group");

    MenuBar menuBar = createMenuBar();

    Div cardHeader = new Div();
    cardHeader.addClassName("header");

    Div controls = new Div();
    controls.addClassName("controls");
    controls.add(menuBar);

    cardHeader.add(title("Experimental Group"));
    cardHeader.add(controls);
    this.add(cardHeader);

    Div cardContent = new Div();
    cardContent.add(condition());
    cardContent.add(sampleSize());
    cardContent.addClassName("content");
    this.add(cardContent);
  }

  private MenuBar createMenuBar() {
    MenuBar menuBar = new MenuBar();
    menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    MenuItem menuItem = menuBar.addItem("•••");
    SubMenu subMenu = menuItem.getSubMenu();
    subMenu.addItem("Edit", event -> {
    });
    subMenu.addItem("Delete", event -> fireDeletionEvent());
    return menuBar;
  }

  private Span title(String value) {
    Span cardTitle = new Span();
    cardTitle.setText(value);
    cardTitle.addClassName("title");
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

  public void fireDeletionEvent() {
    var deletionEvent = new ExperimentalGroupDeletionEvent(ExperimentalGroupCard.this, true);
    listenersDeletionEvent.forEach(listener -> listener.onComponentEvent(deletionEvent));
  }

  public void addDeletionEventListener(
      ComponentEventListener<ExperimentalGroupDeletionEvent> listener) {
    this.listenersDeletionEvent.add(listener);
  }

  public long groupId() {
    return this.experimentalGroup.id();
  }

  public ExperimentalGroup experimentalGroup() {
    return this.experimentalGroup;
  }

  public void subscribeToDeletionEvent(
      ComponentEventListener<ExperimentalGroupDeletionEvent> subscriber) {
    this.listenersDeletionEvent.add(subscriber);
  }

}
