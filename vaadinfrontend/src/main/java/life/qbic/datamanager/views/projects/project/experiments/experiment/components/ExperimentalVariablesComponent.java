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
import java.util.Comparator;
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
  private List<Div> variableFactSheets = new ArrayList<>();
  @Serial
  private static final long serialVersionUID = 7589385115005753849L;
  private final Collection<ExperimentalVariable> experimentalVariables;
  private final MenuBar menuBar;
  private final Div controls = new Div();
  private final Div content = new Div();

  private final List<ComponentEventListener<ExperimentalVariablesEditEvent>> listeners = new ArrayList<>();

  private final List<ComponentEventListener<AddNewExperimentalVariableEvent>> listenersNewVariable = new ArrayList<>();

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
    subMenu.addItem("Add", event -> fireAddEvent());
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

    variableFactSheets.addAll(generateFactSheets(experimentalVariables));
    variableFactSheets.forEach(content::add);

    content.addClassName("content");

    add(content);
  }

  private void fireAddEvent() {
    AddNewExperimentalVariableEvent addNewExperimentalVariableEvent = new AddNewExperimentalVariableEvent(
        this, true);
    listenersNewVariable.forEach(
        listener -> listener.onComponentEvent(addNewExperimentalVariableEvent));
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
    Span variableName = new Span(formatVariableName(experimentalVariable));
    headerSection.addClassName("variable-header");
    headerSection.add(variableName);

    Div variableValues = new Div();
    variableValues.addClassName("variable-values");
    UnorderedList variableLevels = new UnorderedList(experimentalVariable.levels().stream().map(
            VariableLevel::experimentalValue).map(ExperimentalValue::value)
        .sorted(new StringOrNumberComparator()).map(ListItem::new)
        .toArray(ListItem[]::new));
    variableValues.add(variableLevels);
    variableFactSheet.add(headerSection);
    variableFactSheet.add(variableValues);
    return variableFactSheet;
  }

  private static String formatVariableName(ExperimentalVariable experimentalVariable) {
    var unit = experimentalVariable.levels().get(0).experimentalValue().unit();
    return unit.map(s -> experimentalVariable.name().value() + " [" + s + "]")
        .orElseGet(() -> experimentalVariable.name().value());

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
   * @param listener a listener for edit events
   * @since 1.0.0
   */
  public void subscribeToEditEvent(
      ComponentEventListener<ExperimentalVariablesEditEvent> listener) {
    this.listeners.add(listener);
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link AddNewExperimentalVariableEvent}, as soon as a user wants to add new experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public void subscribeToAddEvent(
      ComponentEventListener<AddNewExperimentalVariableEvent> listener) {
    this.listenersNewVariable.add(listener);
  }

  /**
   * Sets a new list of experimental variables.
   * <p>
   * All previously contained variables and fact sheets will be removed.
   *
   * @param variables the new experimental variables to display
   * @since 1.0.0
   */
  public void setExperimentalVariables(List<ExperimentalVariable> variables) {
    this.experimentalVariables.clear();
    this.experimentalVariables.addAll(variables);
    content.removeAll();
    variableFactSheets = generateFactSheets(experimentalVariables);
    variableFactSheets.forEach(content::add);
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

  /*
  Small helper class to nicely sort String or Numbers
   */
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
