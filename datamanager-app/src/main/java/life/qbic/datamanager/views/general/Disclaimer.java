package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Disclaimer</b>
 *
 * <p>Disclaimers are Divs that render a disclaimer to the user,
 * with a clickable confirmation element to either confirm or redirect the user to a follow up
 * step.</p>
 * <p>
 * The component fires a {@link DisclaimerConfirmedEvent} after a user has confirmed the
 * disclaimer.
 * <p>
 * The Div consists of a title that can be set, as well as a disclaimer text and a clickable
 * element that fires the event.
 *
 * @since 1.0.0
 */
public class Disclaimer extends Div {
  @Serial
  private static final long serialVersionUID = -6441310468106881703L;
  private final String disclaimerLabel;
  private final String title;
  private final Button confirmationElement;
  private final List<Registration> confirmListeners;

  private Disclaimer(String disclaimer, String confirmationLabel, String title) {
    this.title = title;
    this.disclaimerLabel = disclaimer;
    this.confirmationElement = new Button(confirmationLabel);
    this.confirmListeners = new ArrayList<>();
    initLayout();
    initConfirmation();
  }

  private void initLayout() {
    addClassName("disclaimer");
    Div content = new Div();
    content.addClassName("disclaimer-content");
    Span titleSpan = new Span();
    titleSpan.add(this.title);
    titleSpan.addClassName("disclaimer-title");
    Paragraph paragraph = new Paragraph(disclaimerLabel);
    paragraph.addClassName("disclaimer-label");

    content.add(titleSpan);
    content.add(paragraph);
    content.add(confirmationElement);
    confirmationElement.addClassName("primary");
    add(content);
  }

  private void initConfirmation() {
    confirmationElement.addClickListener(listener -> fireDisclaimerConfirmedEvent());
  }

  private void fireDisclaimerConfirmedEvent() {
    var event = new DisclaimerConfirmedEvent(this, true);
    fireEvent(event);
  }

  /**
   * Creates a disclaimer without title
   *
   * @param disclaimer        the text shown on the disclaimer
   * @param confirmationLabel the label shown on the confirmation element.
   * @return an instance of a {@link Disclaimer}
   * @since 1.0.0
   */
  public static Disclaimer create(String disclaimer, String confirmationLabel) {
    return new Disclaimer(disclaimer, confirmationLabel, "");
  }

  /**
   * Creates a disclaimer with a title
   *
   * @param disclaimer  the text shown on the disclaimer
   * @param confirmationLabel the label shown on the confirmation element.
   * @param title       the disclaimer title
   * @return an instance of a {@link Disclaimer}
   * @since 1.0.0
   */
  public static Disclaimer createWithTitle(String title, String disclaimer,
      String confirmationLabel) {
    return new Disclaimer(disclaimer, confirmationLabel, title);
  }

  public Registration addDisclaimerConfirmedListener(
      ComponentEventListener<DisclaimerConfirmedEvent> listener) {
    Registration registration = addListener(DisclaimerConfirmedEvent.class, listener);
    confirmListeners.add(registration);
    return registration;
  }

  public void clearConfirmListeners() {
    confirmListeners.forEach(Registration::remove);
    confirmListeners.clear();
  }

}
