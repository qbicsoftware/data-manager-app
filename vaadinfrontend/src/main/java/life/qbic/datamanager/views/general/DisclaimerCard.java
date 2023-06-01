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
 * <b>Disclaimer Card</b>
 *
 * <p>Disclaimer cards are special cards that render a disclaimer to the user,
 * with a clickable button for confirmation.</p>
 * <p>
 * The component fires a {@link DisclaimerConfirmedEvent} after a user has confirmed the
 * disclaimer.
 *
 * @since 1.0.0
 */
public class DisclaimerCard extends Card {

  private List<ComponentEventListener<DisclaimerConfirmedEvent>> listeners;
  @Serial
  private static final long serialVersionUID = -6441310468106881703L;
  private final String disclaimerLabel;

  private final String title;

  private final Button confirmation;

  private DisclaimerCard(String disclaimer, String buttonLabel, String title) {
    this.title = title;
    this.disclaimerLabel = disclaimer;
    this.confirmation = new Button(buttonLabel);
    this.listeners = new ArrayList<>();
    initLayout();
    initConfirmation();
  }

  private void initLayout() {
    addClassName("disclaimer-card");
    Div content = new Div();
    Span titleSpan = new Span();
    titleSpan.add(this.title);
    titleSpan.addClassName("title");
    Paragraph paragraph = new Paragraph(disclaimerLabel);
    paragraph.addClassName("label");

    content.add(titleSpan);
    content.add(paragraph);
    content.add(confirmation);
    confirmation.addClassName("button");
    add(content);
  }

  private void initConfirmation() {
    confirmation.addClickListener(listener -> fireDisclaimerConfirmedEvent());
  }

  public static DisclaimerCard create(String disclaimer, String buttonLabel) {
    return new DisclaimerCard(disclaimer, buttonLabel, "");
  }

  public static DisclaimerCard create(String disclaimer, String buttonLabel, String title) {
    return new DisclaimerCard(disclaimer, buttonLabel, title);
  }

  public void subscribe(ComponentEventListener<DisclaimerConfirmedEvent> listener) {
    this.listeners.add(listener);
  }

  private void fireDisclaimerConfirmedEvent() {
    var event = new DisclaimerConfirmedEvent(this, true);
    listeners.forEach(listener -> listener.onComponentEvent(event));
  }

  public void hideConfirmationButton() {
    this.confirmation.setVisible(false);
  }

  public void displayConfirmationButton() {
    this.confirmation.setVisible(true);
  }

}
