package life.qbic.views;

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
public class ConfirmationMessage extends Composite<VerticalLayout>  {

  private final String descriptionText;
  private final String titleText;

  private Span titleSpan;
  public Span titleTextSpan;

  private Div descriptionDiv;
  public Span descriptionTextSpan;

  public ConfirmationMessage(String titleText, String descriptionText) {

    this.titleText = titleText;
    this.descriptionText = descriptionText;

    initLayout();
    styleLayout();
  }

  private void initLayout() {
    createTitle(titleText);
    createDescriptionText(descriptionText);
  }

  private void styleLayout() {
    styleTitleSpan();
    styleDescriptionDiv();

    this.getContent().add(titleSpan, descriptionDiv);
    this.getContent().addClassNames("p-s", "text-error", "bg-error-10", "rounded-l", "gap-y-s");
  }

  private void createDescriptionText(String descriptionText) {
    this.descriptionTextSpan = new Span(descriptionText);
    descriptionDiv = new Div(this.descriptionTextSpan);
  }

  private void styleDescriptionDiv() {
    this.descriptionDiv.addClassNames("text-left", "mx-l");
  }

  private void createTitle(String titleText) {
    Icon icon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O);
    icon.addClassName("icon-s");
    this.titleTextSpan = new Span(titleText);
    titleSpan = new Span(icon, this.titleTextSpan);
  }

  private void styleTitleSpan() {
    titleSpan.addClassNames("flex", "items-center", "gap-s");
    titleTextSpan.addClassName("font-bold");
  }


}
