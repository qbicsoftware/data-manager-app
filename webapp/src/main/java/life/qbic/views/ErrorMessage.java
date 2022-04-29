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
@CssImport("./styles/views/login/login-view.css")
public class ErrorMessage extends Composite<VerticalLayout> {

  private String descriptionText;
  private String titleText;

  private Span icon;
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
    this.getContent().addClassName("error-10pct");
    this.getContent().getStyle().set("padding", "var(--lumo-space-xs");
  }

  private void createDescriptionText(String descriptionText) {
    this.descriptionTextSpan = new Span(descriptionText);
    descriptionDiv = new Div(this.descriptionTextSpan);
  }

  private void styleDescriptionDiv() {
    descriptionDiv.getStyle().set("padding", "var(--lumo-space-m");
  }

  private void createTitle(String titleText) {
    icon = new Span(new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O));
    this.titleTextSpan = new Span(titleText);
    titleSpan = new Span(icon, this.titleTextSpan);
  }

  private void styleTitleSpan() {
    icon.getStyle().set("padding", "var(--lumo-space-xs");
  }
}
