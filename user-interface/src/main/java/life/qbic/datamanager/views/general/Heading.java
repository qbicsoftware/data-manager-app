package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * <b>Heading (with icon support)</b>
 *
 * @since 1.6.0
 */
public class Heading extends Div {

  private Icon icon;

  private Span text;

  private Heading() {
    addClassName("heading-with-icon");
    this.icon = VaadinIcon.VAADIN_H.create();
    this.text = new Span();
    rebuild();
  }

  public static Heading createEmpty() {
    return new Heading();
  }

  public static Heading withIcon(Icon icon) {
    var headerWithIcon = new Heading();
    headerWithIcon.setIcon(icon);
    return headerWithIcon;
  }

  public static Heading withText(String text) {
    var headerWithText = new Heading();
    headerWithText.setText(text);
    return headerWithText;
  }

  public static Heading withIconAndText(Icon icon, String text) {
    var headerWithIconAndText = new Heading();
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
