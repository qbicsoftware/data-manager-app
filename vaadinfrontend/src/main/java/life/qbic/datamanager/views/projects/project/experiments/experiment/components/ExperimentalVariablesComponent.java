package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.Card;
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
  private final Button addButton;
  private final List<Div> variableFactSheets = new ArrayList<>();

  private ExperimentalVariablesComponent(Collection<ExperimentalVariable> experimentalVariables) {
    this.experimentalVariables = experimentalVariables;
    this.editButton = createEditButton();
    this.addButton = createAddButton();
    addButton.addClassName("primary");
    layoutComponent();
    configureComponent();
  }

  private static Button createEditButton() {
    return new Button("Edit");
  }

  private static Button createAddButton() {
    return new Button("Add");
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
    editButton.addClickListener(
        event -> fireEvent(new ExperimentalVariablesEditEvent(this, event.isFromClient())));
    addButton.addClickListener(
        event -> fireEvent(new ExperimentalVariablesAddEvent(this, event.isFromClient())));
  }

  private void layoutComponent() {
    addClassName("experimental-variables");
    Div cardHeader = new Div();
    cardHeader.addClassName("variables-header");

    controls.addClassName("controls");
    controls.add(editButton, addButton);

    cardHeader.add(new Span("Experimental Variables"));
    cardHeader.add(controls);
    this.add(cardHeader);

    setExperimentalVariables(experimentalVariables);

    content.addClassName("variables-content");
    add(content);
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link ExperimentalVariablesAddEvent}, as soon as a user wants to add new experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public Registration addAddListener(
      ComponentEventListener<ExperimentalVariablesAddEvent> listener) {
    return addListener(ExperimentalVariablesAddEvent.class, listener);
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link ExperimentalVariablesEditEvent}, as soon as a user wants to edit experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public Registration addEditListener(
      ComponentEventListener<ExperimentalVariablesEditEvent> listener) {
    return addListener(ExperimentalVariablesEditEvent.class, listener);
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
