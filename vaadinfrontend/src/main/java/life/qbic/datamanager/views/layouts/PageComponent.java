package life.qbic.datamanager.views.layouts;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderColor;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxSizing;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Flex;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;

/**
 * <b>PageComponent, component hosting the content to be shown on a page in the
 * data-manager-app</b>
 *
 * <p>This component is styled as a card which will host the content to be shown within a page of
 * the data-manager-application</p>
 */
public class PageComponent extends VerticalLayout {

  private final Span layoutTitle = new Span("");
  private final VerticalLayout cardLayout = new VerticalLayout();
  private final VerticalLayout contentLayout = new VerticalLayout();

  public PageComponent() {
    initLayout();
    styleLayout();
  }

  private void initLayout() {
    cardLayout.add(layoutTitle);
    cardLayout.add(contentLayout);
    add(cardLayout);
  }

  private void styleLayout() {
    setDefaultCardStyle();
    setDefaultTitleStyle();
    setAlignItems(Alignment.START);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void setDefaultCardStyle() {
    this.setMaxHeight(100, Unit.PERCENTAGE);
    this.setMaxWidth(100, Unit.PERCENTAGE);
    this.addClassName(Padding.SMALL);
    this.setMargin(false);
    cardLayout.addClassNames(Border.ALL, BorderColor.CONTRAST_10, Flex.AUTO, Padding.LARGE,
        FontSize.SMALL, BoxShadow.MEDIUM, BorderRadius.MEDIUM, BoxSizing.BORDER, Background.BASE);
    cardLayout.setSizeFull();
    contentLayout.setSizeFull();
  }

  private void setDefaultTitleStyle() {
    layoutTitle.addClassNames(FontSize.XXLARGE, FontWeight.BOLD, TextColor.SECONDARY);
    layoutTitle.addClassName(Whitespace.NOWRAP);
    layoutTitle.addClassName(TextOverflow.ELLIPSIS);
    layoutTitle.addClassName(Overflow.HIDDEN);
    layoutTitle.addClassName(Display.INLINE);
  }

  /**
   * Adds the content components to the contentLayout within the PageComponent
   *
   * @param content The content could be a collection of vaadin components
   */
  public void addContent(Component... content) {
    contentLayout.add(content);
  }

  /**
   * Removes specified components from the content of the PageComponent
   */
  public void removeContent(Component... components) {
    contentLayout.remove(components);
  }

  /**
   * Removes all components from the content of the PageComponent
   */
  public void clearContent() {
    contentLayout.removeAll();
  }

  /**
   * Adds a title with a specified text to the card
   *
   * @param text The text for the title
   */
  public void addTitle(String text) {
    layoutTitle.setText(text);
  }

  /**
   * Removes the title from the PageComponent
   */
  public void removeTitle() {
    cardLayout.remove(layoutTitle);
    //No title means no need for indentation of the contentLayout
    indentContent(false);
  }

  /**
   * Defines if the content within the card should be indented in comparison to the tile
   */
  public void indentContent(boolean isIndented) {
    contentLayout.setMargin(isIndented);
    contentLayout.setPadding(isIndented);
  }

  public void setTitleStyles(String... styles) {
    layoutTitle.getClassNames().forEach(layoutTitle::removeClassName);
    layoutTitle.addClassNames(styles);
  }

  public void setCardStyle(String... styles) {
    cardLayout.getClassNames().forEach(this::removeClassName);
    cardLayout.addClassNames(styles);
  }
}
