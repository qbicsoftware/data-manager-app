package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.views.general.CreationCard;
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

  private final List<ExperimentItem> items = new ArrayList<>();
  private final List<ComponentEventListener<ExperimentItemClickedEvent>> listeners = new ArrayList<>();
  private final List<ComponentEventListener<CreationClickedEvent>> createListeners = new ArrayList<>();
  private final CreationCard createExperiment;

  private ExperimentItemCollection(String labelCreationCard) {
    createExperiment = CreationCard.create(labelCreationCard);
    layoutComponent();
  }

  private void layoutComponent() {
    addClassName("experiment-item-collection");
    add(createExperiment);
    addCreationEventListener();
  }

  /**
   * Creates a new {@link ExperimentItemCollection} instance, with a {@link CreationCard}
   * automatically displayed in order to create a new experiment.
   *
   * @param labelCreationCard an intention label that will be shown on the creation card
   * @return a new instance of an {@link ExperimentItemCollection}
   * @since 1.0.0
   */
  public static ExperimentItemCollection create(String labelCreationCard) {
    return new ExperimentItemCollection(labelCreationCard);
  }

  /**
   * Add a new experiment item to the collection.
   *
   * @param item the item to be displayed on the component
   * @since 1.0.0
   */
  public void addExperimentItem(ExperimentItem item) {
    items.add(item);
    addComponentAsFirst(item);
    addSelectionEventListener(item);
  }

  /**
   * Add the given component as the last component in the container. Removes the component from its
   * previous parent.
   *
   * @param component the component to be added
   */
  private void addComponentAsLast(Component component) {
    addComponentAtIndex(getComponentCount(), component);
  }

  private void addSelectionEventListener(ExperimentItem item) {
    item.addSelectionListener(
        (ComponentEventListener<ExperimentItemClickedEvent>) this::fireClickEvent);
  }

  private void addCreationEventListener() {
    createExperiment.addListener(this::fireCreationEvent);
  }

  private void fireCreationEvent(CreationClickedEvent event) {
    createListeners.forEach(createListener -> createListener.onComponentEvent(event));
  }

  private void fireClickEvent(ExperimentItemClickedEvent event) {
    listeners.forEach(listener -> listener.onComponentEvent(event));
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
   * Add a listener of type {@link ComponentEventListener<CreationClickedEvent>} that will be
   * called, if the user intends to create a new experiment.
   *
   * @param listener the listener that will be informed
   * @since 1.0.0
   */
  public void addCreateEventListener(ComponentEventListener<CreationClickedEvent> listener) {
    createListeners.add(listener);
  }

  @Override
  public void removeAll() {
    super.removeAll();
    addComponentAsLast(createExperiment);
    this.items.clear();
  }

}
