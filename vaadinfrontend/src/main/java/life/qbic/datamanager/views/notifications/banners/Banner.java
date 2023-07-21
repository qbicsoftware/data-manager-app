package life.qbic.datamanager.views.notifications.banners;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;

public class Banner extends Div {

  protected final Icon icon;
  protected Component content;

  public Banner(Icon icon, Component content) {
    requireNonNull(icon, "icon must not be null");
    this.icon = icon;
    addClassName("banner");
    add(this.icon);

    if (nonNull(content)) {
      setContent(content);
    }
  }

  public Banner(Icon icon) {
    this(icon, null);
  }

  protected void setContent(Component content) {
    if (nonNull(this.content)) {
      remove(this.content);
    }
    this.content = content;
    this.content.addClassName("content");
    add(this.content);
  }


}
