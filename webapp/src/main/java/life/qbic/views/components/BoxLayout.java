package life.qbic.views.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * <b>Box Layout</b>
 *
 * <p>A box with a shadow containing a title, description, a layout for fields, a layout for buttons and a span to add links.
 * Furthermore, the description text can be toggled visible or invisible
 *
 * @since 1.0.0
 */
public class BoxLayout extends VerticalLayout {

  private H2 layoutTitle;
  private Text descriptionText;
  private VerticalLayout fieldLayout;
  private VerticalLayout textLayout;
  private VerticalLayout buttonLayout;
  private Span linkSpan;

  private final VerticalLayout contentLayout;

  public BoxLayout() {
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

  /**
   * Sets the title text
   * @param text The text for the title
   */
  public void setTitleText(String text) {
    layoutTitle.setText(text);
  }

  /**
   * Adds the field components to the field layout
   * @param fields The fields could be TextFields, EmailFields, PasswordFields
   */
  public void addFields(Component... fields) {
    fieldLayout.add(fields);
  }

  /**
   * Adds buttons to the button layout
   * @param buttons The buttons that need to be part of the layout
   */
  public void addButtons(Button... buttons) {
    buttonLayout.add(buttons);
  }

  /**
   * Sets the description text
   * @param text The text that allows to enter a description of the process
   */
  public void setDescriptionText(String text){
    descriptionText.setText(text);
  }

  /**
   * Toggles the description text visible or invisible
   * @param visible The visibility status of the text
   */
  public void setDescriptionTextVisible(boolean visible){
    descriptionText.setVisible(visible);
  }

  /**
   * Span that will hold content like small texts and links that should be not so present as a button
   * @param components Components like Text, RouterLink, or Tertiary Buttons
   */
  public void addLinkSpanContent(Component... components){
    linkSpan.add(components);
  }
}
