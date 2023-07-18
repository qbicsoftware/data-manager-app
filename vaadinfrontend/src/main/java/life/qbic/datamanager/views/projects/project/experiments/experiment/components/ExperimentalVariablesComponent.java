package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.CreationCard;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;

/**
 * <b>Experiment Variable Component</b>
 *
 * <p>Component that displays experimental variable information and enables the user
 * to modify existing variables for a given experiment.</p>
 *
 * @since 1.0.0
 */
public class ExperimentalVariablesComponent extends Card {

  @Serial
  private static final long serialVersionUID = 7589385115005753849L;
  private final Collection<ExperimentalVariable> experimentalVariables;
  private final Div controls = new Div();
  private final Div content = new Div();
  private final Button editButton;
  private final CreationCard variableCreationCard;


  private final List<ComponentEventListener<ExperimentalVariablesEditEvent>> listeners = new ArrayList<>();
  private final List<ComponentEventListener<AddNewExperimentalVariableEvent>> listenersNewVariable = new ArrayList<>();
  private final List<Div> variableFactSheets = new ArrayList<>();

  private ExperimentalVariablesComponent(Collection<ExperimentalVariable> experimentalVariables) {
    this.experimentalVariables = experimentalVariables;
    this.editButton = createEditButton("Edit");
    variableCreationCard = CreationCard.create("Add experimental variables");
    variableCreationCard.addListener(event -> fireAddEvent());
    layoutComponent();
    configureComponent();
  }

  private static Button createEditButton(String label) {
    Button button = new Button(label);
    return button;
  }
  private void addComponentAsLast(Component component) {
    content.addComponentAtIndex(content.getComponentCount(), component);
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

  private static List<VariableFactSheet> generateFactSheets(
      Collection<ExperimentalVariable> experimentalVariables) {
    return experimentalVariables.stream()
        .map(VariableFactSheet::new)
        .toList();
  }

  public static ExperimentalVariablesComponent create(
      Collection<ExperimentalVariable> experimentalVariables) {
    Objects.requireNonNull(experimentalVariables);
    return new ExperimentalVariablesComponent(experimentalVariables);
  }

  private void configureComponent() {
    editButton.addClickListener(event -> fireEditEvent());
  }

  private void layoutComponent() {
    addClassName("experimental-variables");
    Div cardHeader = new Div();
    cardHeader.addClassName("header");

    controls.addClassName("controls");
    controls.add(editButton);

    cardHeader.add(new Span("Experimental Variables"));
    cardHeader.add(controls);
    this.add(cardHeader);

    setExperimentalVariables(experimentalVariables);

    content.addClassName("content");
    add(content);
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
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link ExperimentalVariablesEditEvent}, as soon as a user wants to edit experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public void subscribeToEditEvent(
      ComponentEventListener<ExperimentalVariablesEditEvent> listener) {
    this.listeners.add(listener);
  }

  /**
   * Sets a new list of experimental variables.
   * <p>
   * All previously contained variables and fact sheets will be removed.
   *
   * @param variables the new experimental variables to display
   * @since 1.0.0
   */
  public void setExperimentalVariables(Collection<ExperimentalVariable> variables) {
    resetContent();
    this.experimentalVariables.addAll(variables);
    variableFactSheets.addAll(generateFactSheets(experimentalVariables));
    variableFactSheets.forEach(content::add);
    addComponentAsLast(variableCreationCard);
  }

  private void resetContent() {
    this.experimentalVariables.clear();
    this.variableFactSheets.clear();
    content.removeAll();
  }


  /*
  Small helper class to nicely sort String or Numbers
   */


}
