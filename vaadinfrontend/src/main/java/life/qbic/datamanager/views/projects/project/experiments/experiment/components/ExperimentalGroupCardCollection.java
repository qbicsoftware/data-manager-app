package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ClickEvent;
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
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupCard;

/**
 * <b>Experimental Group Collection</b>
 * <p>
 * Container of one or more {@link ExperimentalGroupCard}
 *
 * @since 1.0.0
 */

public class ExperimentalGroupCardCollection extends Div {

  @Serial
  private static final long serialVersionUID = -5835580091959912561L;

  private final Div content = new Div();
  private final List<ComponentEventListener<AddEvent<ExperimentalGroupCardCollection>>> addListeners = new ArrayList<>();
  private final List<ComponentEventListener<EditEvent<ExperimentalGroupCardCollection>>> editListeners = new ArrayList<>();


  public ExperimentalGroupCardCollection() {
    addClassName("experimental-group-card-collection");
    Span title = new Span("Groups");
    title.setClassName("title");
    Div header = new Div();
    header.setClassName("header");
    Div controlItems = new Div();
    controlItems.setClassName("controls");
    content.setClassName("content");
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

  public void setContent(Collection<ExperimentalGroupCard> experimentalGroupComponents) {
    content.removeAll();
    experimentalGroupComponents.forEach(content::add);
  }

  public void addAddEventListener(
      ComponentEventListener<AddEvent<ExperimentalGroupCardCollection>> listener) {
    this.addListeners.add(listener);
  }

  public void addEditEventListener(
      ComponentEventListener<EditEvent<ExperimentalGroupCardCollection>> listener) {
    this.editListeners.add(listener);
  }

  private void fire(EditEvent<ExperimentalGroupCardCollection> editEvent) {
    this.editListeners.forEach(listener -> listener.onComponentEvent(editEvent));
  }

  private void fire(AddEvent<ExperimentalGroupCardCollection> addEvent) {
    this.addListeners.forEach(listener -> listener.onComponentEvent(addEvent));
  }

}
