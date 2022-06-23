package life.qbic.views.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * <b>short description</b>
 *
 * <p>detailed description
 *
 * @since <version tag>
 */
public class BoxForm extends VerticalLayout {

  private H2 layoutTitle;
  private Text descriptionText;
  private VerticalLayout fieldLayout;
  private VerticalLayout textLayout;
  private VerticalLayout buttonLayout;
  private Span linkSpan;

  private final VerticalLayout contentLayout;

  public BoxForm() {
    this.addClassName("grid");
    contentLayout = new VerticalLayout();

    initLayout();
    styleLayout();
  }

  private void initLayout() {
    layoutTitle = new H2("Set Title");

    textLayout = new VerticalLayout();
    descriptionText = new Text("Enter description text");
    textLayout.add(descriptionText);

    fieldLayout = new VerticalLayout();
    buttonLayout = new VerticalLayout();

    linkSpan = new Span();
    add(contentLayout);
  }

  private void styleLayout() {
    styleFieldLayout();
    styleFormLayout();
    styleButtonLayout();
    styleDescriptionText();

    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void styleFormLayout() {
    contentLayout.setPadding(false);
    contentLayout.setMargin(false);
    contentLayout.addClassNames(
        "bg-base",
        "border",
        "rounded-m",
        "border-contrast-10",
        "box-border",
        "flex",
        "flex-col",
        "w-full",
        "text-s",
        "shadow-l",
        "min-width-300px",
        "max-width-15vw",
        "pb-l",
        "pr-l",
        "pl-l");
    contentLayout.add(layoutTitle, descriptionText, fieldLayout, buttonLayout, linkSpan);
  }

  private void styleFieldLayout() {
    fieldLayout.setSpacing(false);
    fieldLayout.setMargin(false);
    fieldLayout.setPadding(false);
  }

  private void styleButtonLayout(){
    buttonLayout.setSpacing(false);
    buttonLayout.setMargin(false);
    buttonLayout.setPadding(false);
  }

  private void styleDescriptionText(){
    textLayout.addClassName("text-contrast-70");
  }

  public void setTitleText(String text) {
    layoutTitle.setText(text);
  }

  public void addFields(Component... fields) {
    fieldLayout.add(fields);
  }

  public void addButtons(Button... buttons) {
    buttonLayout.add(buttons);
  }

  public void setDescriptionText(String text){
    descriptionText.setText(text);
  }

  public void setDescriptionTextVisible(boolean visible){
    descriptionText.setVisible(visible);
  }

  public void addLinkSpanContent(Component... components){
    linkSpan.add(components);
  }
}
