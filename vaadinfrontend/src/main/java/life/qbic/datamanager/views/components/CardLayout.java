package life.qbic.datamanager.views.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * <b>Card Layout</b>
 *
 * <p>A card with a shadow containing a title, description, a layout for fields, a layout for buttons and a span to add links.
 * Furthermore, the description text can be toggled visible or invisible
 *
 * @since 1.0.0
 */
public class CardLayout extends VerticalLayout {

  private Label layoutTitle;
  private HorizontalLayout headerLayout;
  private HorizontalLayout buttonLayout;
  private final VerticalLayout contentLayout;
  private VerticalLayout fieldLayout;

  public CardLayout() {
    contentLayout = new VerticalLayout();
    initLayout();
    styleLayout();
  }

  private void initLayout() {
    layoutTitle = new Label("Set Title");
    headerLayout = new HorizontalLayout();
    buttonLayout = new HorizontalLayout();
    headerLayout.add(layoutTitle, buttonLayout);
    fieldLayout = new VerticalLayout();
    add(contentLayout);
  }

  private void styleLayout() {
    styleCardLayout();
    styleHeaderLayout();
    styleLayoutTitle();
    styleButtonLayout();
    styleFieldLayout();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void styleCardLayout() {
    contentLayout.setPadding(false);
    contentLayout.setMargin(false);
    contentLayout.setWidthFull();
    contentLayout.setHeightFull();
    contentLayout.addClassNames(
        "bg-base",
        "border",
        "rounded-m",
        "border-contrast-10",
        "box-border",
        "flex",
        "flex-col",
        "text-s",
        "shadow-l",
        "pb-l",
        "pr-l",
        "pl-l");
    contentLayout.add(headerLayout, fieldLayout);
  }

  //ToDo rename styling methods to setDefaultStyling method
  private void styleLayoutTitle(){
    layoutTitle.addClassNames(
        "text-2xl",
        "text-header",
        "font-bold"
        );
  }

  private void styleHeaderLayout(){
    headerLayout.setSpacing(false);
    headerLayout.setMargin(false);
    headerLayout.setPadding(false);
    headerLayout.addClassNames(
        "mt-m",
        "mb-s"
    );
    headerLayout.setWidthFull();
    headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
  }

  private void styleButtonLayout(){
    buttonLayout.setMargin(false);
    buttonLayout.setPadding(false);
    buttonLayout.setSpacing(false);
    buttonLayout.setClassName("gap-s");
  }

  private void styleFieldLayout() {
    fieldLayout.setSpacing(false);
    fieldLayout.setMargin(false);
    fieldLayout.setPadding(false);
    fieldLayout.setHeightFull();
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
   * Removes all DisplayMessage {@link DisplayMessage} based Notifications from the BoxLayout
   */
  //Todo Add methods to allow styling of components in card
}
