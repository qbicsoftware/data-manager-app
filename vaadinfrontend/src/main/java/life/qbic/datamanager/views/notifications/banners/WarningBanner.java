package life.qbic.datamanager.views.notifications.banners;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class WarningBanner extends Banner {


  public WarningBanner(String text) {
    super(VaadinIcon.WARNING.create(), new Span(text));
    addClassName("warning");
  }
}
