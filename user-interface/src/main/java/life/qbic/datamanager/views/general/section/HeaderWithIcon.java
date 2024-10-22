package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.theme.lumo.LumoIcon;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class HeaderWithIcon extends Div {

  private Icon icon;

  private String text;

  private HeaderWithIcon() {
    addClassName("header-with-icon");
    this.icon = LumoIcon.DOWNLOAD.create();
  }

  public static HeaderWithIcon createEmpty() {
    return new HeaderWithIcon();
  }

  public HeaderWithIcon withIcon(Icon icon) {
    var headerWithIcon = new HeaderWithIcon();
    headerWithIcon.setIcon(icon);
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
