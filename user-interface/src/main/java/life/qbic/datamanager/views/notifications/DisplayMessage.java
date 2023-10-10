package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * <b> A DisplayMessage component which shows an unspecific styled information message with a
 * title and a detailed description. </b>
 *
 * @since 1.0.0
 */
public class DisplayMessage extends Composite<VerticalLayout> {

  protected Span titleSpan;
  public Span titleTextSpan;

  protected Div descriptionDiv;
  public Span descriptionTextSpan;
  public Span iconSpan;
  protected Icon messageIcon;

  public DisplayMessage(String titleText, String descriptionText) {
    titleTextSpan = new Span(titleText);
    descriptionTextSpan = new Span(descriptionText);

    configureIcon();
    iconSpan = new Span(messageIcon);
    titleSpan = new Span(iconSpan, this.titleTextSpan);
    descriptionDiv = new Div(this.descriptionTextSpan);
    getContent().add(titleSpan, descriptionDiv);

    styleCommonLayout();
    styleSpecificLayout();

    this.getContent().add(titleSpan, descriptionDiv);
  }

  protected void configureIcon() {
    messageIcon = new Icon(VaadinIcon.ASTERISK);
    messageIcon.addClassName("icon-s");
  }

  private void styleCommonLayout() {
    titleSpan.addClassNames("flex", "items-top", "gap-s");
    titleTextSpan.addClassName("font-bold");
    this.descriptionDiv.addClassNames("text-left", "mx-l");
  }

  protected void styleSpecificLayout() {
    getContent().addClassNames("p-s", "bg-contrast-5", "rounded-l", "gap-y-s");
  }

  public String title() {
    return titleTextSpan.getText();
  }

  public String message() {
    return descriptionTextSpan.getText();
  }
}
