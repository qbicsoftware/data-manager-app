package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.Card;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * <b>Experiment Variable Component</b>
 *
 * <p>Component that displays experimental variable information and enables the user
 * to modify existing variables for a given experiment.</p>
 *
 * @since 1.0.0
 */
public class ExperimentalVariablesComponent extends Card {

  private boolean controlsEnabled = true;
  @Serial
  private static final long serialVersionUID = 7589385115005753849L;
  private final Collection<ExperimentalVariable> experimentalVariables;
  private final MenuBar menuBar;
  private final Div controls = new Div();

  private final List<ComponentEventListener<ExperimentalVariablesEditEvent>> listeners = new ArrayList<>();

  private ExperimentalVariablesComponent(Collection<ExperimentalVariable> experimentalVariables) {
    this.experimentalVariables = experimentalVariables;
    this.menuBar = createMenuBar();
    layoutComponent();
  }

  private MenuBar createMenuBar() {
    MenuBar menu = new MenuBar();
    menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
    MenuItem menuItem = menu.addItem("•••");
    SubMenu subMenu = menuItem.getSubMenu();
    subMenu.addItem("Edit", event -> fireEditEvent());
    return menu;
  }

  private void layoutComponent() {
    addClassName("experimental-variables");
    Div cardHeader = new Div();
    cardHeader.addClassName("header");

    controls.addClassName("controls");
    controls.add(menuBar);

    cardHeader.add(new Span("Experimental Variables"));
    cardHeader.add(controls);
    this.add(cardHeader);

    Div content = new Div();

    List<Div> variableFactSheets = generateFactSheets(experimentalVariables);
    variableFactSheets.forEach(content::add);

    content.addClassName("content");

    add(content);
  }

  private void fireEditEvent() {
    var editEvent = new ExperimentalVariablesEditEvent(this, true);
    listeners.forEach(listener -> listener.onComponentEvent(editEvent));
  }

  private static List<Div> generateFactSheets(
      Collection<ExperimentalVariable> experimentalVariables) {
    return experimentalVariables.stream().map(ExperimentalVariablesComponent::generateFactSheet)
        .toList();
  }

  private static Div generateFactSheet(ExperimentalVariable experimentalVariable) {
    Div variableFactSheet = new Div();
    variableFactSheet.addClassName("experimental-variables-fact-sheet");
    Div headerSection = new Div();
    Span variableName = new Span(experimentalVariable.name().value());
    headerSection.addClassName("variable-header");
    headerSection.add(variableName);

    Div variableValues = new Div();
    variableValues.addClassName("variable-values");
    UnorderedList variableLevels = new UnorderedList(experimentalVariable.levels().stream().map(
            VariableLevel::experimentalValue).map(ExperimentalValue::value).map(ListItem::new)
        .toArray(ListItem[]::new));
    variableValues.add(variableLevels);
    variableFactSheet.add(headerSection);
    variableFactSheet.add(variableValues);
    return variableFactSheet;
  }

  public static ExperimentalVariablesComponent create(
      Collection<ExperimentalVariable> experimentalVariables) {
    Objects.requireNonNull(experimentalVariables);
    return new ExperimentalVariablesComponent(experimentalVariables);
  }

  /**
   * Register an {@link ComponentEventListener} that will get informed with a
   * {@link ExperimentalVariablesEditEvent}, as soon as a user wants to edit experiment variables.
   *
   * @param listener A listener for edit events
   * @since 1.0.0
   */
  public void subscribeToEditEvent(
      ComponentEventListener<ExperimentalVariablesEditEvent> listener) {
    this.listeners.add(listener);
  }

  /**
   * Adds and displays the control menu to the component
   *
   * @since 1.0.0
   */
  public void showMenu() {
    if (!controlsEnabled) {
      addControlMenu();
      controlsEnabled = true;
    }
  }

  private void addControlMenu() {
    controls.add(menuBar);
  }

  /**
   * Removes the control menu from the component.
   *
   * @since 1.0.0
   */
  public void hideMenu() {
    if (controlsEnabled) {
      removeControlMenu();
      controlsEnabled = false;
    }
  }

  private void removeControlMenu() {
    controls.remove(menuBar);
  }

}
