package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import java.util.ArrayList;
import java.util.List;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class CreationCard extends Card {

  private final List<ComponentEventListener<CreationClickedEvent>> listeners;

  private Button addButton;

  public CreationCard(Button button) {
    this.addButton = button;
    this.listeners = new ArrayList<>();
    add(addButton);
    addClassName("creation-card");
    setupEvents();
  }

  private void setupEvents() {
    this.addButton.addClickListener(listener -> {
      CreationClickedEvent creationClickedEvent = new CreationClickedEvent(this, true);
      listeners.forEach(eventListener -> eventListener.onComponentEvent(creationClickedEvent));
    });
  }

  public static CreationCard create() {
    return create("Add");
  }

  public static CreationCard create(String label) {
    return new CreationCard(new Button(label));
  }

  public void addListener(ComponentEventListener<CreationClickedEvent> listener) {
    this.listeners.add(listener);
  }
}
