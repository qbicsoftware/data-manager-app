package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import life.qbic.datamanager.views.general.AddEvent;
import life.qbic.datamanager.views.general.EditEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalVariableCard;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;

/**
 * <b>Experimental Variable Collection</b>
 * <p>
 * Container of one or more {@link ExperimentalVariableCard}
 *
 * @since 1.0.0
 */

public class ExperimentalVariableCardCollection extends Div {

  @Serial
  private static final long serialVersionUID = -9123769128332512326L;

  private final Div content = new Div();
  private final List<ComponentEventListener<AddEvent<ExperimentalVariableCardCollection>>> addListeners = new ArrayList<>();
  private final List<ComponentEventListener<EditEvent<ExperimentalVariableCardCollection>>> editListeners = new ArrayList<>();


  public ExperimentalVariableCardCollection() {
    addClassName("experimental-group-card-collection");
    Span title = new Span("Variables");
    title.addClassName("title");
    Div header = new Div();
    header.addClassName("groups-header");
    Div controlItems = new Div();
    controlItems.addClassName("groups-controls");
    content.addClassName("groups-content");
    header.add(title, controlItems);
    Button addButton = new Button("Add");
    Button editButton = new Button("Edit");
    controlItems.add(editButton, addButton);

    addButton.addClassName("primary");
    add(header, content);

    addButton.addClickListener(this::emitAddEvent);
    editButton.addClickListener(this::emitEditEvent);
  }

  private void emitEditEvent(ClickEvent<Button> buttonClickEvent) {
    var editEvent = new EditEvent<>(this, true);
    fire(editEvent);
  }

  private void emitAddEvent(ClickEvent<Button> buttonClickEvent) {
    var addEvent = new AddEvent<>(this, true);
    fire(addEvent);
  }

  /**
   * Removes all components from the content section and sets the provided components as content.
   *
   * @param components the components to set to the content section
   */
  public void setContent(Collection<? extends Component> components) {
    content.removeAll();
    components.forEach(content::add);
  }

  /**
   * Adds a listener to the add event
   * @param listener the listener to add
   */
  public void addAddEventListener(
      ComponentEventListener<AddEvent<ExperimentalVariableCardCollection>> listener) {
    this.addListeners.add(listener);
  }

  /**
   * Adds a listener to an EditEvent
   * @param listener the listener to add
   */
  public void addEditEventListener(
      ComponentEventListener<EditEvent<ExperimentalVariableCardCollection>> listener) {
    this.editListeners.add(listener);
  }

  private void fire(EditEvent<ExperimentalVariableCardCollection> editEvent) {
    this.editListeners.forEach(listener -> listener.onComponentEvent(editEvent));
  }

  private void fire(AddEvent<ExperimentalVariableCardCollection> addEvent) {
    this.addListeners.forEach(listener -> listener.onComponentEvent(addEvent));
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link AddEvent<ExperimentalVariableCardCollection>}, as soon as a user wants to add new experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public void subscribeToAddEvent(
      ComponentEventListener<AddEvent<ExperimentalVariableCardCollection>> listener) {
    this.addListeners.add(listener);
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link EditEvent<ExperimentalVariableCardCollection>}, as soon as a user wants to edit experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public void subscribeToEditEvent(
      ComponentEventListener<EditEvent<ExperimentalVariableCardCollection>> listener) {
    this.editListeners.add(listener);
  }

}
