package life.qbic.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * <b> An ErrorMessage component which shows an error message with a title and a detailed
 * description. </b>
 *
 * @since 1.0.0
 */
public class ErrorMessage extends Composite<VerticalLayout> {

  private String descriptionText;
  private String titleText;

  private Span titleSpan;
  public Span titleTextSpan;

  private Div descriptionDiv;
  public Span descriptionTextSpan;

  public ErrorMessage(String titleText, String descriptionText) {

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
    this.getContent().addClassNames("p-xs", "text-error", "bg-error-10");
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
    titleSpan.addClassNames("flex", "items-center", "gap-xs");
    titleTextSpan.addClassName("font-bold");
  }
}
