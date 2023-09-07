package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.views.general.CreationClickedEvent;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * <b>Experiment Items Collection</b>
 * <p>
 * Component that lists experiments of a project and enables the user to quickly switch between them
 * for management.
 * <p>
 * The component enables the client to register to {@link ExperimentItem.ExperimentItemClickedEvent} to listen to
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

  private ExperimentItemCollection() {
    addClassName("experiment-item-collection");
    layoutComponent();
  }

  private void layoutComponent() {
    initHeader();
    content.addClassName("content");
    add(content);
  }

  private void initHeader() {
    Div controls = new Div();
    controls.addClassName("controls");
    Button addButton = new Button("Add");
    addButton.addClassName("primary");
    addButton.addClickListener(
        event -> fireEvent(new AddExperimentClickEvent(this, event.isFromClient())));
    controls.add(addButton);
    Span title = new Span("Experiments");
    title.addClassName("title");
    header.add(title, controls);
    header.addClassName("header");
    addComponentAsFirst(header);
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
    item.addClickListener(event -> fireEvent(
        new ExperimentSelectionEvent(this, event.isFromClient(), event.getExperimentId())));
    items.add(item);
    content.addComponentAsFirst(item);
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

  @Override
  public void removeAll() {
    content.removeAll();
    items.clear();
  }

  public void addAddButtonListener(ComponentEventListener<AddExperimentClickEvent> listener) {
    addListener(AddExperimentClickEvent.class, listener);
  }

  public void addExperimentSelectionListener(
      ComponentEventListener<ExperimentSelectionEvent> listener) {
    addListener(ExperimentSelectionEvent.class, listener);
  }

  public static class AddExperimentClickEvent extends ComponentEvent<ExperimentItemCollection> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public AddExperimentClickEvent(ExperimentItemCollection source, boolean fromClient) {
      super(source, fromClient);
    }
  }


  public static class ExperimentSelectionEvent extends ComponentEvent<ExperimentItemCollection> {

    private final ExperimentId experimentId;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source       the source component
     * @param fromClient   <code>true</code> if the event originated from the client
     *                     side, <code>false</code> otherwise
     * @param experimentId the selected experiment
     */
    public ExperimentSelectionEvent(ExperimentItemCollection source, boolean fromClient,
        ExperimentId experimentId) {
      super(source, fromClient);
      this.experimentId = experimentId;
    }

    public ExperimentId getExperimentId() {
      return experimentId;
    }
  }


}
