package life.qbic.datamanager.views.notifications.banners;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * A warning banner implementation with some text.
 */
public class WarningBanner extends Banner {


  public WarningBanner(String text) {
    super(VaadinIcon.WARNING.create(), new Span(text));
    addClassName("warning");
  }
}
