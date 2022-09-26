package life.qbic.datamanager.views.components;

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
 * <p>A card with a shadow containing a title, description, a layout for fields, a layout for
 * buttons and a span to add links.
 * Furthermore, the description text can be toggled visible or invisible
 *
 * @since 1.0.0
 */
@CssImport("./styles/components/card-layout.css")
public class CardLayout extends VerticalLayout {

  private Label layoutTitle;

  private VerticalLayout contentLayout;
  private VerticalLayout leftLayout;
  private VerticalLayout rightLayout;
  private HorizontalLayout buttonLayout;
  private VerticalLayout fieldLayout;

  public CardLayout() {
    initLayout();
    styleLayout();
  }

  private void initLayout() {
    layoutTitle = new Label("Set Title");
    contentLayout = new VerticalLayout();
    buttonLayout = new HorizontalLayout();
    fieldLayout = new VerticalLayout();
    leftLayout = new VerticalLayout();
    rightLayout = new VerticalLayout();
    leftLayout.add(layoutTitle, fieldLayout);
    rightLayout.add(buttonLayout);
    contentLayout.add(leftLayout, rightLayout);
    add(contentLayout);
  }

  private void styleLayout() {
    setDefaultCardLayoutStyle();
    setDefaultTitleStyle();
    setDefaultButtonLayoutStyle();
    setDefaultFieldLayoutStyle();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void setDefaultCardLayoutStyle() {
    this.setSizeFull();
    contentLayout.setSizeFull();
    contentLayout.addClassNames(
        "min-size-to-content",
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
        "p-xl",
        "m-xl"
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
    layoutTitle.addClassNames(
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
   * Sets the title text
   *
   * @param text The text for the title
   */
  public void setTitleText(String text) {
    layoutTitle.setText(text);
  }

  /**
   * Adds the field components to the field layout
   *
   * @param fields The fields could be TextFields, EmailFields, PasswordFields
   */
  public void addFields(Component... fields) {
    fieldLayout.add(fields);
  }

  /**
   * Adds buttons to the button layout
   *
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
