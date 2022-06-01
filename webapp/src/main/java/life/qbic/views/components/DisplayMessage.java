package life.qbic.views.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DisplayMessage extends Composite<VerticalLayout> {
  protected Span titleSpan;
  public Span titleTextSpan;

  protected Div descriptionDiv;
  public Span descriptionTextSpan;

  protected Icon messageIcon;

  public DisplayMessage(String titleText, String descriptionText) {
    titleTextSpan = new Span(titleText);
    descriptionTextSpan = new Span(descriptionText);

    configureIcon();

    titleSpan = new Span(messageIcon, this.titleTextSpan);
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
    titleSpan.addClassNames("flex", "items-center", "gap-s");
    titleTextSpan.addClassName("font-bold");
    this.descriptionDiv.addClassNames("text-left", "mx-l");
  }

  protected void styleSpecificLayout() {
    getContent().addClassNames("p-s", "text-error", "bg-error-10", "rounded-l", "gap-y-s");
  }
}
