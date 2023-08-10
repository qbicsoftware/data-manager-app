package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.views.general.AddEvent;
import life.qbic.datamanager.views.general.CreationClickedEvent;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * <b>Experiment Items Collection</b>
 * <p>
 * Component that lists experiments of a project and enables the user to quickly switch between them
 * for management.
 * <p>
 * The component enables the client to register to {@link ExperimentItemClickedEvent} to listen to
 * experiment selections by the user and {@link CreationClickedEvent}, indicating the user wants to
 * create a new experiment.
 *
 * @since 1.0.0
 */
@Tag(Tag.DIV)
public class ExperimentItemCollection extends Div {

  private final Div header = new Div();
  private final Div content = new Div();
  private final List<ExperimentItem> items = new ArrayList<>();
  private final List<ComponentEventListener<ExperimentItemClickedEvent>> listeners = new ArrayList<>();
  private final List<ComponentEventListener<AddEvent<ExperimentItemCollection>>> addListeners = new ArrayList<>();

  private ExperimentItemCollection() {
    addClassName("experiment-item-collection");
    layoutComponent();
  }

  private void layoutComponent() {
    initHeader();
    content.addClassName("content");
    this.add(content);
  }

  private void initHeader() {
    Div controls = new Div();
    controls.addClassName("controls");
    Button addButton = new Button("Add");
    controls.add(addButton);
    addButton.addClickListener(this::emitAddEvent);
    Span title = new Span("Experiments");
    title.addClassName("title");
    header.add(title, controls);
    header.addClassName("header");
    this.addComponentAsFirst(header);
  }

  private void emitAddEvent(ClickEvent<Button> buttonClickEvent) {
    var addEvent = new AddEvent<>(this, true);
    fireAddEvent(addEvent);
  }

  /**
   * Creates a new {@link ExperimentItemCollection} instance
   *
   * @return a new instance of an {@link ExperimentItemCollection}
   * @since 1.0.0
   */
  public static ExperimentItemCollection create() {
    return new ExperimentItemCollection();
  }

  /**
   * Add a new experiment item to the collection.
   *
   * @param item the item to be displayed on the component
   * @since 1.0.0
   */
  public void addExperimentItem(ExperimentItem item) {
    items.add(item);
    content.addComponentAsFirst(item);
    addSelectionEventListener(item);
  }

  private void addSelectionEventListener(ExperimentItem item) {
    item.addSelectionListener(
        (ComponentEventListener<ExperimentItemClickedEvent>) this::fireClickEvent);
  }

  private void fireClickEvent(ExperimentItemClickedEvent event) {
    listeners.forEach(listener -> listener.onComponentEvent(event));
  }

  private void fireAddEvent(AddEvent<ExperimentItemCollection> addEvent) {
    this.addListeners.forEach(listener -> listener.onComponentEvent(addEvent));
  }

  /**
   * Queries for an experiment item that represents an experiment with a provided
   * {@link ExperimentId}.
   *
   * @param id the id of the experiment to search the matching {@link ExperimentItem}
   * @return an optional search result hit
   * @since 1.0.0
   */
  public Optional<ExperimentItem> findBy(ExperimentId id) {
    return items.stream().filter(experimentItem -> experimentItem.experimentId().equals(id))
        .findAny();
  }

  /**
   * Add a listener of type {@link ComponentEventListener<ExperimentItemClickedEvent>} that will be
   * called, if an {@link ExperimentItemClickedEvent} is emitted.
   *
   * @param listener the listener that will be informed
   * @since 1.0.0
   */
  public void addClickEventListener(ComponentEventListener<ExperimentItemClickedEvent> listener) {
    listeners.add(listener);
  }

  /**
   * Add a listener of type {@link ComponentEventListener<AddEvent<ExperimentItemCollection>} that will be
   * called, if the user intends to create a new experiment.
   *
   * @param listener the listener that will be informed
   * @since 1.0.0
   */
  public void addAddEventListener(
      ComponentEventListener<AddEvent<ExperimentItemCollection>> listener) {
    this.addListeners.add(listener);
  }

  @Override
  public void removeAll() {
    content.removeAll();
    this.items.clear();
  }

}
