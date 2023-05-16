package life.qbic.datamanager.views.layouts;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;

/**
 * <b>CardComponent, component hosting the content to be shown on a card within the
 * {@link PageComponent} in the data-manager-app</b>
 *
 * <p>This component is styled as a card which will host the content of interest within a
 * {@link PageComponent}t in the data-manager-application</p>
 *
 * @since 1.0.0
 */
public class CardComponent extends VerticalLayout {

  //Vaadin Boards and Flex Grids have difficulties to account for margins between components,
  //which is why we need a dedicated cardLayout around the card itself which provides margins and paddings
  private final VerticalLayout cardLayout = new VerticalLayout();
  private final VerticalLayout contentLayout = new VerticalLayout();
  private final Span layoutTitle = new Span("");

  public CardComponent() {
    HorizontalLayout titleLayout = new HorizontalLayout();
    cardLayout.add(titleLayout);
    titleLayout.add(layoutTitle);
    cardLayout.add(contentLayout);
    setDefaultCardStyle();
    setDefaultTitleStyle();
    add(cardLayout);
  }

  private void setDefaultCardStyle() {
    cardLayout.addClassNames(Border.ALL, BorderColor.CONTRAST_10, Flex.AUTO, Padding.LARGE,
        FontSize.SMALL, BoxShadow.MEDIUM, BorderRadius.MEDIUM, BoxSizing.BORDER, Background.BASE);
    cardLayout.setSizeFull();
    contentLayout.setSizeFull();
    this.addClassName(Padding.SMALL);
    this.setMargin(false);
    this.setMaxHeight(100, Unit.PERCENTAGE);
    this.setMaxWidth(100, Unit.PERCENTAGE);
  }

  private void setDefaultTitleStyle() {
    layoutTitle.addClassNames("text-2xl", "font-bold", "text-secondary");
    layoutTitle.addClassName(Whitespace.NOWRAP);
    layoutTitle.addClassName(TextOverflow.ELLIPSIS);
    layoutTitle.addClassName(Overflow.HIDDEN);
    layoutTitle.addClassName(Display.INLINE);
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
   * Removes the title from the CardComponent
   */
  public void removeTitle() {
    cardLayout.remove(layoutTitle);
    //No title means no need for indentation of the contentLayout
    indentContent(false);
  }

  /**
   * Adds the content components to the contentLayout within the CardComponent
   *
   * @param content The content could be a collection of vaadin components
   */
  public void addContent(Component... content) {
    contentLayout.add(content);
  }

  public void indentContent(boolean isIndented) {
    contentLayout.setMargin(isIndented);
    contentLayout.setPadding(isIndented);
  }

  /**
   * Removes specified components from the content of the CardComponent
   */
  public void removeContent(Component... components) {
    contentLayout.remove(components);
  }

  /**
   * Removes all components from the content of the CardComponent
   */
  public void clearContent() {
    contentLayout.removeAll();
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
