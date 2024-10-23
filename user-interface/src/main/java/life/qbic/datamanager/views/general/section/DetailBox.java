package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DetailBox extends Div {

  private Div headerSection;

  private Div contentSection;

  private Header header;

  private Component content;

  public DetailBox() {
    addClassName("detail-box");
    headerSection = new Div();
    headerSection.addClassName("detail-box-child");
    contentSection = new Div();
    contentSection.addClassName("detail-box-child");
    add(headerSection, contentSection);
  }

  public void setHeader(Header header) {
    this.header = header;
    rebuild();
  }

  public void setContent(Component content) {
    this.content = content;
    rebuild();
  }

  private void rebuild() {
    headerSection.removeAll();
    contentSection.removeAll();
    if (header != null) {
      headerSection.add(header);
    }
    if (content != null) {
      add(content);
      contentSection.add(content);
    }
  }


  public static class Header extends Div {

    private boolean iconVisible = false;

    private Icon icon;

    private Div heading;

    public Header() {
      addClassName("detail-box-header");
      heading = new Div();
    }

    public Header(Icon icon, String text) {
      this();
      this.icon = icon;
      heading.setText(text);
      displayIcon();
      rebuild();
    }

    private void displayIcon () {
      this.iconVisible = true;
    }

    private void hideTheIcon () {
      this.iconVisible = false;
    }

    public void showIcon() {
      displayIcon();
      rebuild();
    }

    public void hideIcon() {
      hideTheIcon();
      rebuild();
    }

    public Header(String text) {
      this();
      heading.setText(text);
      rebuild();
    }

    private void rebuild() {
      removeAll();
      if (iconVisible && icon != null) {
        add(icon);
      }
      if (heading != null) {
        add(heading);
      }
    }
  }
}
