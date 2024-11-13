package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;

/**
 * <b>Detail Box</b>
 * <p>
 * A data manager detail box contains of the two main layout sections:
 *
 * <ul>
 *   <li>Header Section</li>
 *   <li>Content Section</li>
 * </ul>
 * <p>
 * Detail boxes are used to visually highlight some contextual information to the user,
 * with a descriptive heading, icons and some border to separate it from the surrounding elements.
 * <p>
 * Developer hint: the content section can be filled with any content, but the height is currently
 * restricted to 10rem (css: detail-box). Then the overflow will trigger a scrollbar in the content section.
 *
 * @since 1.6.0
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
    contentSection.addClassName("overflow-scroll-height");
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
      showIcon();
      rebuild();
    }

    public Header(String text) {
      this();
      heading.setText(text);
      rebuild();
    }

    private void setIconVisibility(boolean visible) {
      iconVisible = visible;
    }

    public void showIcon() {
      setIconVisibility(true);
      rebuild();
    }

    public void hideIcon() {
      setIconVisibility(false);
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
