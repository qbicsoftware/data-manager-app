package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Creation Card</b>
 * <p>
 * A card component that can be used to indicate creation of something in the application
 *
 * @since 1.0.0
 */
public class CreationCard extends Card {

  @Serial
  private static final long serialVersionUID = 7298407268081888530L;
  private final List<ComponentEventListener<CreationClickedEvent>> listeners;

  private final Div content;

  private CreationCard(String label) {
    this.listeners = new ArrayList<>();
    content = styleLayout(label);
    add(content);
    setupEvents();
  }

  private Div styleLayout(String label) {
    var div = new Div();
    addClassName("creation-card");
    Icon addIcon = new Icon(VaadinIcon.PLUS);
    div.add(addIcon);
    div.add(new H5(label));
    return div;
  }

  private void setupEvents() {
    this.content.addClickListener(listener -> {
      CreationClickedEvent creationClickedEvent = new CreationClickedEvent(this, true);
      listeners.forEach(eventListener -> eventListener.onComponentEvent(creationClickedEvent));
    });
  }

  public static CreationCard create() {
    return create("Add");
  }

  public static CreationCard create(String label) {
    return new CreationCard(label);
  }

  public void addListener(ComponentEventListener<CreationClickedEvent> listener) {
    this.listeners.add(listener);
  }
}
