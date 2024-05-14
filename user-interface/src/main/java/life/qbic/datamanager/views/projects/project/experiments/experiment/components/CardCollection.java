package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.util.Collection;
import life.qbic.datamanager.views.general.Card;

/**
 * <b>Collection of elements (cards) for which more than one should usually be shown</b>
 * <p>
 * Container of one or more {@link Card}s. Provides means to subscribe to and fire edit and add events.
 *
 * @since 1.0.0
 */

public class CardCollection extends Div {

  @Serial
  private static final long serialVersionUID = -9123769128332512326L;

  private final Div content = new Div();
  private final Span controls;

  public CardCollection(String title) {
    addClassName("card-collection");
    Span titleSpan = new Span(title);
    titleSpan.addClassName("collection-title");
    Div header = new Div();
    header.addClassName("collection-header");
    Button addButton = new Button("Add");
    Button editButton = new Button("Edit");
    addButton.addClickListener(this::fireAddEvent);
    editButton.addClickListener(this::fireEditEvent);
    content.addClassName("collection-content");
    controls = new Span(editButton, addButton);
    controls.addClassName("collection-controls");
    header.add(titleSpan, controls);
    addButton.addClassName("primary");
    add(header, content);
  }

  public void showControls(boolean isShown) {
    controls.setVisible(isShown);
  }

  private void fireEditEvent(ClickEvent<Button> buttonClickEvent) {
    var editEvent = new EditEvent(this, buttonClickEvent.isFromClient());
    fireEvent(editEvent);
  }

  private void fireAddEvent(ClickEvent<Button> buttonClickEvent) {
    var addEvent = new AddEvent(this, buttonClickEvent.isFromClient());
    fireEvent(addEvent);
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
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link AddEvent< CardCollection >}, as soon as a user wants to add new experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public void addAddListener(
      ComponentEventListener<AddEvent> listener) {
    addListener(AddEvent.class, listener);
  }

  /**
   * Register a {@link ComponentEventListener} that will get informed with an
   * {@link EditEvent< CardCollection >}, as soon as a user wants to edit experimental
   * variables.
   *
   * @param listener a listener for adding variables events
   * @since 1.0.0
   */
  public void addEditListener(
      ComponentEventListener<EditEvent> listener) {
    addListener(EditEvent.class, listener);
  }

  /**
   * <b>Edit Event</b>
   *
   * <p>Indicates that a user wants to edit a card collection</p>
   *
   * @since 1.0.0
   */
  public static class EditEvent extends ComponentEvent<CardCollection> {

    @Serial
    private static final long serialVersionUID = -7777255533105234741L;

    public EditEvent(CardCollection source, boolean fromClient) {
      super(source, fromClient);
    }
  }


  /**
   * <b>Add Event</b>
   *
   * <p>Indicates that a user wants to add a card to the collection</p>
   *
   * @since 1.0.0
   */
  public static class AddEvent extends ComponentEvent<CardCollection> {

    private static final long serialVersionUID = -1156260489115426107L;

    public AddEvent(CardCollection source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
