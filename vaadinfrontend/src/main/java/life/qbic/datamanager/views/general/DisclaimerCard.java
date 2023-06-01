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
 * with a clickable confirmation element to either confirm or redirect the user to a follow up
 * step.</p>
 * <p>
 * The component fires a {@link DisclaimerConfirmedEvent} after a user has confirmed the
 * disclaimer.
 * <p>
 * The card consists of a title that can be set, as well as a disclaimer text and a clickable
 * element that fires the event.
 * <p>
 * The confirmation element can be enabled (default) or disabled.
 *
 * @since 1.0.0
 */
public class DisclaimerCard extends Card {

  private List<ComponentEventListener<DisclaimerConfirmedEvent>> listeners;
  @Serial
  private static final long serialVersionUID = -6441310468106881703L;
  private final String disclaimerLabel;

  private final String title;

  private final Button confirmationElement;

  private DisclaimerCard(String disclaimer, String confirmationLabel, String title) {
    this.title = title;
    this.disclaimerLabel = disclaimer;
    this.confirmationElement = new Button(confirmationLabel);
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
    content.add(confirmationElement);
    confirmationElement.addClassName("button");
    add(content);
  }

  private void initConfirmation() {
    confirmationElement.addClickListener(listener -> fireDisclaimerConfirmedEvent());
  }

  private void fireDisclaimerConfirmedEvent() {
    var event = new DisclaimerConfirmedEvent(this, true);
    listeners.forEach(listener -> listener.onComponentEvent(event));
  }

  /**
   * Creates a disclaimer card without title
   *
   * @param disclaimer        the disclaimer shown on the card
   * @param confirmationLabel the label shown on the confirmation element.
   * @return an instance of a {@link DisclaimerCard}
   * @since 1.0.0
   */
  public static DisclaimerCard create(String disclaimer, String confirmationLabel) {
    return new DisclaimerCard("", disclaimer, confirmationLabel);
  }

  /**
   * Creates a disclaimer card with a title
   *
   * @param disclaimer  the disclaimer shown on the card
   * @param confirmationLabel the label shown on the confirmation element.
   * @param title       the card title
   * @return an instance of a {@link DisclaimerCard}
   * @since 1.0.0
   */
  public static DisclaimerCard createWithTitle(String title, String disclaimer, String confirmationLabel) {
    return new DisclaimerCard(disclaimer, confirmationLabel, title);
  }

  public void subscribe(ComponentEventListener<DisclaimerConfirmedEvent> listener) {
    this.listeners.add(listener);
  }

  public void disableConfirmation() {
    this.confirmationElement.setVisible(false);
  }

  public void enableConfirmation() {
    this.confirmationElement.setVisible(true);
  }

}
