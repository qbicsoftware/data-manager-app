package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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

  private final Button createButton = new Button("Add");

  private CreationCard(String label) {
    this.listeners = new ArrayList<>();
    styleLayout(label);
    setupEvents();
  }

  private void styleLayout(String label) {
    var content = new Div();
    addClassName("creation-card");


    Span disclaimer = new Span(label);
    Paragraph disclaimerArea = new Paragraph(disclaimer);
    disclaimerArea.add(disclaimer);

    disclaimerArea.addClassName("disclaimer-area");
    disclaimer.addClassName("disclaimer");

    createButton.addClassName("primary");

    content.add(disclaimerArea);
    content.add(createButton);
    add(content);
  }

  private void setupEvents() {
    this.createButton.addClickListener(listener -> {
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
