package life.qbic.datamanager.views.layouts;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * <b>Card Layout</b>
 *
 * <p>A card Component with a shadow containing customizable components such as a title, a layout
 * for fields and a layout for buttons.
 *
 * @since 1.0.0
 */
@CssImport("./styles/components/card-layout.css")
public class CardLayout extends VerticalLayout {

  private Label layoutTitle;
  private VerticalLayout contentLayout;
  private VerticalLayout leftLayout;
  private VerticalLayout rightLayout;
  private HorizontalLayout titleLayout;
  private HorizontalLayout buttonLayout;
  private VerticalLayout fieldLayout;

  public CardLayout() {
    initLayout();
    styleLayout();
  }

  private void initLayout() {
    contentLayout = new VerticalLayout();
    leftLayout = new VerticalLayout();
    rightLayout = new VerticalLayout();
    buttonLayout = new HorizontalLayout();
    fieldLayout = new VerticalLayout();
    titleLayout = new HorizontalLayout();
    layoutTitle = new Label("");
    titleLayout.add(layoutTitle);
    leftLayout.add(titleLayout, fieldLayout);
    rightLayout.add(buttonLayout);
    contentLayout.add(leftLayout, rightLayout);
    add(contentLayout);
  }

  private void styleLayout() {
    setCardLayoutStyle();
    setDefaultTitleStyle();
    setDefaultButtonLayoutStyle();
    setDefaultFieldLayoutStyle();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void setCardLayoutStyle() {
    this.setSizeFull();
    contentLayout.setSizeFull();
    contentLayout.addClassNames(
        "bg-base",
        "border",
        "rounded-m",
        "border-contrast-10",
        "box-border",
        "rounded-m",
        "flex",
        "flex-row",
        "text-s",
        "shadow-l",
        "p-l"
    );
    leftLayout.setPadding(false);
    leftLayout.setMargin(false);
    leftLayout.setAlignItems(Alignment.START);
    leftLayout.setSizeFull();
    rightLayout.setPadding(false);
    rightLayout.setMargin(false);
    rightLayout.setAlignItems(Alignment.END);
    rightLayout.setSizeUndefined();
  }

  private void setDefaultTitleStyle() {
    titleLayout.addClassNames(
        "text-2xl",
        "font-bold",
        "text-secondary"
    );
  }

  private void setDefaultButtonLayoutStyle() {
    buttonLayout.setMargin(false);
    buttonLayout.setPadding(false);
    buttonLayout.setSpacing(false);
    buttonLayout.setClassName("gap-s");
  }

  private void setDefaultFieldLayoutStyle() {
    fieldLayout.setSpacing(false);
    fieldLayout.setMargin(false);
    fieldLayout.setPadding(false);
    fieldLayout.setHeightFull();
  }

  /**
   * Adds buttons to the button layout within the CardLayout
   *
   * @param buttons The buttons that need to be part of the layout
   */
  public void addButtons(Button... buttons) {
    buttonLayout.add(buttons);
  }

  /**
   * Adds the field components to the field layout within the CardLayout
   *
   * @param fields The fields could be a collection of vaadin components
   */
  public void addFields(Component... fields) {
    fieldLayout.add(fields);
  }

  /**
   * Adds a title with a specified text to the card
   *
   * @param text The text for the title
   */
  public void addTitle(String text) {
    if (!titleLayout.getChildren().toList().contains(layoutTitle)) {
      titleLayout.add(layoutTitle);
    }
    layoutTitle.setText(text);
  }

  /**
   * Removes specified buttons from the CardLayout
   */
  public void removeButtons(Button... buttons) {
    buttonLayout.remove(buttons);
  }

  /**
   * Removes specified fields from the CardLayout
   */
  public void removeFields(Component... components) {
    fieldLayout.remove(components);
  }

  /**
   * Removes the title from the CardLayout
   */
  public void removeTitle() {
    titleLayout.removeAll();
  }

  /**
   * Adds custom css styling to the ButtonLayout within the CardLayout
   *
   * @param buttonStyles String representations of vaadin css classNames that will be added to the
   *                     buttonLayout
   */
  public void addButtonStyles(String... buttonStyles) {
    buttonLayout.addClassNames(buttonStyles);
  }

  /**
   * Adds custom css styling to the fieldLayout within the CardLayout
   *
   * @param fieldStyles String representations of vaadin css classNames that will be added to the
   *                    fieldLayout
   */
  public void addFieldStyles(String... fieldStyles) {
    fieldLayout.addClassNames(fieldStyles);
  }

  /**
   * Adds custom css styling to the fieldLayout within the CardLayout
   *
   * @param titleStyles String representations of vaadin css classNames that will be removed from
   *                    the titleLayout
   */
  public void addTitleStyles(String... titleStyles) {
    titleLayout.addClassNames(titleStyles);
  }

  /**
   * Removes specified css styling of the buttonLayout within the CardLayout
   *
   * @param buttonStyles String representations of vaadin css classNames that will be removed from
   *                     the buttonLayout
   */
  public void removeButtonStyles(String... buttonStyles) {
    buttonLayout.removeClassNames(buttonStyles);
  }

  /**
   * Removes specified css styling of the fieldLayout within the CardLayout
   *
   * @param fieldStyles String representations of vaadin css classNames that will be removed from
   *                    the fieldLayout
   */
  public void removeFieldStyles(String... fieldStyles) {
    fieldLayout.removeClassNames(fieldStyles);
  }

  /**
   * Removes specified css styling of the titleLayout within the CardLayout
   *
   * @param titleStyles String representations of vaadin css classNames that will be added to the
   *                    titleLayout
   */
  public void removeTitleStyles(String... titleStyles) {
    titleLayout.removeClassNames(titleStyles);
  }

  /**
   * Removes all styling of the buttonLayout within the CardLayout
   */
  public void removeAllButtonStyles() {
    String[] currentButtonLayoutStyles = buttonLayout.getClassNames().toArray(String[]::new);
    buttonLayout.removeClassNames(currentButtonLayoutStyles);
  }

  /**
   * Removes all styling of the fieldLayout within the CardLayout
   */
  public void removeAllFieldStyles() {
    String[] currentFieldLayoutStyles = fieldLayout.getClassNames().toArray(String[]::new);
    fieldLayout.removeClassNames(currentFieldLayoutStyles);
  }

  /**
   * Removes all styling of the titleLayout within the CardLayout
   */
  public void removeAllTitleStyles() {
    String[] currentTitleLayoutStyles = titleLayout.getClassNames().toArray(String[]::new);
    titleLayout.removeClassNames(currentTitleLayoutStyles);
  }

}
