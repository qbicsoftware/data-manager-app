package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class HeadingWithIcon extends Div {

  private Icon icon;

  private Span text;

  private HeadingWithIcon() {
    addClassName("heading-with-icon");
    this.icon = VaadinIcon.VAADIN_H.create();
    this.text = new Span();
    rebuild();
  }

  public static HeadingWithIcon createEmpty() {
    return new HeadingWithIcon();
  }

  public static HeadingWithIcon withIcon(Icon icon) {
    var headerWithIcon = new HeadingWithIcon();
    headerWithIcon.setIcon(icon);
    return headerWithIcon;
  }

  public static HeadingWithIcon withText(String text) {
    var headerWithText = new HeadingWithIcon();
    headerWithText.setText(text);
    return headerWithText;
  }

  public static HeadingWithIcon withIconAndText(Icon icon, String text) {
    var headerWithIconAndText = new HeadingWithIcon();
    headerWithIconAndText.setIcon(icon);
    headerWithIconAndText.setCustomText(text);
    return headerWithIconAndText;
  }

  private void setCustomText(String text) {
    this.text = new Span(text);
    rebuild();
  }

  public void setIcon(Icon icon) {
    this.icon = icon;
    rebuild();
  }

  private void rebuild() {
    removeAll();
    add(icon);
    add(text);
  }


}
